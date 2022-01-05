package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManagerImpl;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.SyncImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.attributes.AttributeTaskFactoryImpl;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.TelemetrySynchronizerImpl;
import io.split.android.client.telemetry.TelemetrySynchronizerStub;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.ApiKeyValidator;
import io.split.android.client.validators.ApiKeyValidatorImpl;
import io.split.android.client.validators.AttributesValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.KeyValidatorImpl;
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

    private final FactoryMonitor _factoryMonitor = FactoryMonitorImpl.getSharedInstance();
    private final SplitLifecycleManager _lifecyleManager;
    private final SyncManager _syncManager;
    private final SplitRoomDatabase _splitDatabase;

    public SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config, Context context)
            throws URISyntaxException {
        this(apiToken, key, config, context,
                null, null, null);
    }

    private SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config,
                             Context context, HttpClient httpClient, SplitRoomDatabase testDatabase,
                             SynchronizerSpy synchronizerSpy)
            throws URISyntaxException {

        long initializationStartTime = System.currentTimeMillis();
        SplitFactoryHelper factoryHelper = new SplitFactoryHelper();
        setupValidations(config);
        ApiKeyValidator apiKeyValidator = new ApiKeyValidatorImpl();
        KeyValidator keyValidator = new KeyValidatorImpl();
        ValidationMessageLogger validationLogger = new ValidationMessageLoggerImpl();

        HttpClient defaultHttpClient;
        if (httpClient == null) {
            defaultHttpClient = new HttpClientImpl.Builder()
                    .setConnectionTimeout(config.connectionTimeout())
                    .setReadTimeout(config.readTimeout())
                    .setProxy(config.proxy())
                    .setDevelopmentSslConfig(config.developmentSslConfig())
                    .setContext(context)
                    .setProxyAuthenticator(config.authenticator()).build();
        } else {
            defaultHttpClient = httpClient;
        }

        ValidationErrorInfo errorInfo = keyValidator.validate(key.matchingKey(), key.bucketingKey());
        String validationTag = "factory instantiation";
        if (errorInfo != null) {
            validationLogger.log(errorInfo, validationTag);
        }

        errorInfo = apiKeyValidator.validate(apiToken);
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

        // Check if test database available
        String databaseName = factoryHelper.getDatabaseName(config, apiToken, context);
        if (testDatabase == null) {
            _splitDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        } else {
            _splitDatabase = testDatabase;
            Logger.d("Using test database");
        }

        defaultHttpClient.addHeaders(factoryHelper.buildHeaders(config, apiToken));
        defaultHttpClient.addStreamingHeaders(factoryHelper.buildStreamingHeaders(apiToken));

        SplitEventsManager _eventsManager = new SplitEventsManager(config);

        SplitTaskExecutor _splitTaskExecutor = new SplitTaskExecutorImpl();

        SplitStorageContainer storageContainer = factoryHelper.buildStorageContainer(_splitDatabase, key, config.shouldRecordTelemetry());

        SplitParser splitParser = new SplitParser(storageContainer.getMySegmentsStorage());

        String splitsFilterQueryString = factoryHelper.buildSplitsFilterQueryString(config);
        SplitApiFacade splitApiFacade = factoryHelper.buildApiFacade(
                config, key, defaultHttpClient, splitsFilterQueryString);

        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(
                config, splitApiFacade, storageContainer, splitsFilterQueryString, _eventsManager);

        cleanUpDabase(_splitTaskExecutor, splitTaskFactory);

        Synchronizer synchronizer = new SynchronizerImpl(
                config, _splitTaskExecutor, storageContainer, splitTaskFactory,
                _eventsManager, factoryHelper.buildWorkManagerWrapper(
                context, config, apiToken, key.matchingKey(), databaseName), new RetryBackoffCounterTimerFactory());

        // Only available for integration tests
        if (synchronizerSpy != null) {
            synchronizerSpy.setSynchronizer(synchronizer);
            synchronizer = synchronizerSpy;
        }

        _syncManager = factoryHelper.buildSyncManager(key.matchingKey(), config, _splitTaskExecutor,
                splitTaskFactory, splitApiFacade, defaultHttpClient, synchronizer, _eventsManager, getTelemetrySynchronizer(_splitTaskExecutor,
                        splitTaskFactory,
                        config.telemetryRefreshRate(),
                        config.shouldRecordTelemetry()));

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
                    customerImpressionListener.close();
                    Logger.i("Successful shutdown of ImpressionListener");
                    defaultHttpClient.close();
                    Logger.i("Successful shutdown of httpclient");
                    _manager.destroy();
                    Logger.i("Successful shutdown of manager");
                    _splitTaskExecutor.stop();
                    Logger.i("Successful shutdown of task executor");
                    storageContainer.getAttributesStorage().destroy();
                    Logger.i("Successful shutdown of attributes storage");
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

        _client = new SplitClientImpl(this,
                key,
                splitParser,
                customerImpressionListener,
                config,
                _eventsManager,
                storageContainer.getSplitsStorage(),
                new EventPropertiesProcessorImpl(),
                _syncManager,
                getAttributesManager(config.persistentAttributesEnabled(), validationLogger, _splitTaskExecutor, storageContainer),
                storageContainer.getTelemetryStorage(),
                new SplitValidatorImpl());

        _manager = new SplitManagerImpl(
                storageContainer.getSplitsStorage(),
                new SplitValidatorImpl(), splitParser);

        _eventsManager.getExecutorResources().setSplitClient(_client);

        if (config.shouldRecordTelemetry()) {
            int activeFactoriesCount = _factoryMonitor.count(_apiKey);
            storageContainer.getTelemetryStorage().recordActiveFactories(activeFactoriesCount);
            storageContainer.getTelemetryStorage().recordRedundantFactories(activeFactoriesCount - 1);

            _client.on(SplitEvent.SDK_READY, new SplitEventTask() {
                @Override
                public void onPostExecution(SplitClient client) {
                    storageContainer.getTelemetryStorage().recordTimeUntilReady(System.currentTimeMillis() - initializationStartTime);
                }
            });

            _client.on(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
                @Override
                public void onPostExecution(SplitClient client) {
                    storageContainer.getTelemetryStorage().recordTimeUntilReadyFromCache(System.currentTimeMillis() - initializationStartTime);
                }
            });
        }

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

    private void cleanUpDabase(SplitTaskExecutor splitTaskExecutor,
                               SplitTaskFactory splitTaskFactory) {
        splitTaskExecutor.submit(splitTaskFactory.createCleanUpDatabaseTask(System.currentTimeMillis() / 1000), null);
    }

    @NonNull
    private AttributesManagerImpl getAttributesManager(boolean persistentAttributesEnabled, ValidationMessageLogger validationLogger, SplitTaskExecutor _splitTaskExecutor, SplitStorageContainer storageContainer) {
        if (persistentAttributesEnabled) {
            return new AttributesManagerImpl(storageContainer.getAttributesStorage(),
                    new AttributesValidatorImpl(),
                    validationLogger,
                    storageContainer.getPersistentAttributesStorage(),
                    new AttributeTaskFactoryImpl(),
                    _splitTaskExecutor);
        }

        return new AttributesManagerImpl(storageContainer.getAttributesStorage(),
                new AttributesValidatorImpl(),
                validationLogger);
    }

    @NonNull
    private TelemetrySynchronizer getTelemetrySynchronizer(SplitTaskExecutor _splitTaskExecutor,
                                                           SplitTaskFactory splitTaskFactory,
                                                           long telemetryRefreshRate,
                                                           boolean shouldRecordTelemetry) {
        if (shouldRecordTelemetry) {
            return new TelemetrySynchronizerImpl(_splitTaskExecutor, splitTaskFactory, telemetryRefreshRate);
        } else {
            return new TelemetrySynchronizerStub();
        }
    }
}
