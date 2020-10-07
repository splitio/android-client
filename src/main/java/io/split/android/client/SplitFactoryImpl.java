package io.split.android.client;

import android.content.Context;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.split.android.client.api.Key;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.cache.MySegmentsCacheMigrator;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.cache.SplitCacheMigrator;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.SyncImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.metrics.CachedMetrics;
import io.split.android.client.metrics.FireAndForgetMetrics;
import io.split.android.client.metrics.HttpMetrics;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.migrator.EventsMigratorHelper;
import io.split.android.client.storage.db.migrator.EventsMigratorHelperImpl;
import io.split.android.client.storage.db.migrator.ImpressionsMigratorHelper;
import io.split.android.client.storage.db.migrator.ImpressionsMigratorHelperImpl;
import io.split.android.client.storage.db.migrator.MySegmentsMigratorHelper;
import io.split.android.client.storage.db.migrator.MySegmentsMigratorHelperImpl;
import io.split.android.client.storage.db.migrator.SplitsMigratorHelper;
import io.split.android.client.storage.db.migrator.SplitsMigratorHelperImpl;
import io.split.android.client.storage.db.migrator.StorageMigrator;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.FileStorageHelper;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.storage.legacy.ImpressionsFileStorage;
import io.split.android.client.storage.legacy.ImpressionsStorageManager;
import io.split.android.client.storage.legacy.ImpressionsStorageManagerConfig;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.client.storage.legacy.TracksFileStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.validators.ApiKeyValidator;
import io.split.android.client.validators.ApiKeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationConfig;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;

public class SplitFactoryImpl implements SplitFactory {

    private final SplitClient _client;
    private final SplitManager _manager;
    private final Runnable destroyer;
    private boolean isTerminated = false;
    private final String _apiKey;

    private FactoryMonitor _factoryMonitor = FactoryMonitorImpl.getSharedInstance();
    private SplitLifecycleManager _lifecyleManager;
    private SyncManager _syncManager;

    public SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config, Context context)
            throws URISyntaxException {
        this(apiToken, key, config, context,
                new HttpClientImpl.Builder()
                        .setConnectionTimeout(config.connectionTimeout())
                        .setReadTimeout(config.readTimeout())
                        .setProxy(config.proxy())
                        .enableSslDevelopmentMode(config.isSslDevelopmentModeEnabled())
                        .setContext(context)
                        .setProxyAuthenticator(config.authenticator()).build());
    }

    private SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config,
                             Context context, HttpClient httpClient)
            throws URISyntaxException {

        SplitFactoryHelper factoryHelper = new SplitFactoryHelper();
        setupValidations(config);
        ApiKeyValidator apiKeyValidator = new ApiKeyValidatorImpl();
        ValidationMessageLogger validationLogger = new ValidationMessageLoggerImpl();

        ValidationErrorInfo errorInfo = apiKeyValidator.validate(apiToken);
        String validationTag = "factory instantiation";
        if (errorInfo != null) {
            validationLogger.log(errorInfo, validationTag);
        }

        int factoryCount = _factoryMonitor.count(apiToken);
        if (factoryCount > 0) {
            validationLogger.w("You already have " + factoryCount + (factoryCount == 1 ? " factory" : " factories") + "with this API Key. We recommend keeping only " +
                    "one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        } else if (_factoryMonitor.count() > 0) {
            validationLogger.w("You already have an instance of the Split factory. Make sure you definitely want this additional instance. We recommend " +
                    "keeping only one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        }
        _factoryMonitor.add(apiToken);
        _apiKey = apiToken;

        String databaseName = factoryHelper.buildDatabaseName(config, apiToken);
        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        checkAndMigrateIfNeeded(context.getCacheDir(), databaseName, splitRoomDatabase);

        httpClient.addHeaders(factoryHelper.buildHeaders(config, apiToken));

        URI eventsRootTarget = URI.create(config.eventsEndpoint());

        HttpMetrics httpMetrics = HttpMetrics.create(httpClient, eventsRootTarget);
        final FireAndForgetMetrics uncachedFireAndForget = FireAndForgetMetrics.instance(httpMetrics, 2, 1000);

        SplitEventsManager _eventsManager = new SplitEventsManager(config);

        SplitStorageContainer storageContainer = factoryHelper.buildStorageContainer(context, key, factoryHelper.buildDatabaseName(config, apiToken));

        SplitParser splitParser = new SplitParser(storageContainer.getMySegmentsStorage());

        // TODO: Setup metrics in task executor
        CachedMetrics cachedMetrics = new CachedMetrics(httpMetrics, TimeUnit.SECONDS.toMillis(config.metricsRefreshRate()));
        final FireAndForgetMetrics cachedFireAndForgetMetrics = FireAndForgetMetrics.instance(cachedMetrics, 2, 1000);


        String splitsFilterQueryString = factoryHelper.buildSplitsFilterQueryString(config);
        SplitApiFacade splitApiFacade = factoryHelper.buildApiFacade(
                config, key, httpClient, cachedFireAndForgetMetrics, splitsFilterQueryString);

        SplitTaskExecutor _splitTaskExecutor = new SplitTaskExecutorImpl();
        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(
                config, splitApiFacade, storageContainer, key.matchingKey(), splitsFilterQueryString);

        Synchronizer synchronizer = new SynchronizerImpl(
                config, _splitTaskExecutor, storageContainer, splitTaskFactory,
                _eventsManager, factoryHelper.buildWorkManagerWrapper(
                context, config, apiToken, key.matchingKey(), databaseName));

        _syncManager = factoryHelper.buildSyncManager(key.matchingKey(), config, _splitTaskExecutor,
                splitTaskFactory, splitApiFacade, httpClient, synchronizer);

        _syncManager.start();

        final ImpressionListener splitImpressionListener
                = new SyncImpressionListener(_syncManager);
        final ImpressionListener customerImpressionListener;

        if (config.impressionListener() != null) {
            List<ImpressionListener> impressionListeners = new ArrayList<>();
            impressionListeners.add(splitImpressionListener);
            impressionListeners.add(config.impressionListener());
            customerImpressionListener = new ImpressionListener.FederatedImpressionListener(impressionListeners);
        } else {
            customerImpressionListener = splitImpressionListener;
        }

        _lifecyleManager = new SplitLifecycleManager();
        _lifecyleManager.register(_syncManager);

        destroyer = new Runnable() {
            public void run() {
                Logger.w("Shutdown called for split");
                try {
                    _syncManager.stop();
                    Logger.i("Flushing impressions and events");
                    _lifecyleManager.destroy();
                    Logger.i("Successful shutdown of lifecycle manager");
                    _factoryMonitor.remove(_apiKey);
                    Logger.i("Successful shutdown of segment fetchers");
                    uncachedFireAndForget.close();
                    Logger.i("Successful shutdown of metrics 1");
                    cachedFireAndForgetMetrics.close();
                    Logger.i("Successful shutdown of metrics 2");
                    customerImpressionListener.close();
                    Logger.i("Successful shutdown of ImpressionListener");
                    httpClient.close();
                    Logger.i("Successful shutdown of httpclient");
                    _manager.destroy();
                    Logger.i("Successful shutdown of manager");

                    _splitTaskExecutor.stop();
                    Logger.i("Successful shutdown of task executor");

                } catch (Exception e) {
                    Logger.e(e, "We could not shutdown split");
                } finally {
                    isTerminated = true;
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Using the full path to avoid conflicting with Thread.destroy()
                SplitFactoryImpl.this.destroy();
            }
        });

        _client = new SplitClientImpl(this, key, splitParser,
                customerImpressionListener, cachedFireAndForgetMetrics, config, _eventsManager,
                storageContainer.getSplitsStorage(), new EventPropertiesProcessorImpl(),
                _syncManager);
        _manager = new SplitManagerImpl(
                storageContainer.getSplitsStorage(),
                new SplitValidatorImpl(), splitParser);

        _eventsManager.getExecutorResources().setSplitClient(_client);

        Logger.i("Android SDK initialized!");
    }

    public SplitClient client() {
        return _client;
    }

    public SplitManager manager() {
        return _manager;
    }

    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (!isTerminated) {
                new Thread(destroyer).start();
            }
        }
    }

    @Override
    public void flush() {
        _syncManager.flush();
    }

    @Override
    public boolean isReady() {
        return _client.isReady();
    }

    private void setupValidations(SplitClientConfig splitClientConfig) {

        ValidationConfig.getInstance().setMaximumKeyLength(splitClientConfig.maximumKeyLength());
        ValidationConfig.getInstance().setTrackEventNamePattern(splitClientConfig.trackEventNamePattern());
    }

    private void checkAndMigrateIfNeeded(File rootFolder,
                                         String dataFolderName,
                                         SplitRoomDatabase splitRoomDatabase) {

        StorageMigrator storageMigrator = new StorageMigrator(splitRoomDatabase);
        if (!storageMigrator.isMigrationDone()) {
            Logger.i("Migrating cache to new storage implementation");
            IStorage fileStore = new FileStorage(rootFolder, dataFolderName);
            SplitCacheMigrator splitCacheMigrator = new SplitCache(fileStore);
            MySegmentsCacheMigrator mySegmentsCacheMigrator = new MySegmentsCache(fileStore);

            TracksFileStorage tracksFileStorage = new TracksFileStorage(rootFolder, dataFolderName);
            TrackStorageManager trackStorageManager = new TrackStorageManager(tracksFileStorage);


            ImpressionsFileStorage impressionsFileStorage = new ImpressionsFileStorage(rootFolder, dataFolderName);
            ImpressionsStorageManager impressionsStorageManager =
                    new ImpressionsStorageManager(impressionsFileStorage,
                            new ImpressionsStorageManagerConfig(),
                            new FileStorageHelper());

            SplitsMigratorHelper splitsMigratorHelper
                    = new SplitsMigratorHelperImpl(splitCacheMigrator);
            MySegmentsMigratorHelper mySegmentsMigratorHelper
                    = new MySegmentsMigratorHelperImpl(mySegmentsCacheMigrator, new StringHelper());

            EventsMigratorHelper eventsMigratorHelper = new EventsMigratorHelperImpl(trackStorageManager);

            ImpressionsMigratorHelper impressionsMigratorHelper =
                    new ImpressionsMigratorHelperImpl(impressionsStorageManager);

            storageMigrator.runMigration(mySegmentsMigratorHelper, splitsMigratorHelper,
                    eventsMigratorHelper, impressionsMigratorHelper);

            Logger.i("Migration done");

        }
    }
}
