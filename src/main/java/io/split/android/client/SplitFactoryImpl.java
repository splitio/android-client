package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.SyncImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.shared.SplitClientContainerImpl;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.TelemetrySynchronizerImpl;
import io.split.android.client.telemetry.TelemetrySynchronizerStub;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.ApiKeyValidator;
import io.split.android.client.validators.ApiKeyValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationConfig;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;

public class SplitFactoryImpl implements SplitFactory {

    private final Key mDefaultClientKey;
    private final SplitManager mManager;
    private final Runnable mDestroyer;
    private boolean mIsTerminated = false;
    private final String mApiKey;

    private final FactoryMonitor mFactoryMonitor = FactoryMonitorImpl.getSharedInstance();
    private final SplitLifecycleManager mLifecycleManager;
    private final SyncManager mSyncManager;

    private final SplitStorageContainer mStorageContainer;
    private final SplitClientContainer mClientContainer;

    public SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config, Context context)
            throws URISyntaxException {
        this(apiToken, key, config, context,
                null, null, null);
    }

    private SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config,
                             Context context, HttpClient httpClient, SplitRoomDatabase testDatabase,
                             SynchronizerSpy synchronizerSpy)
            throws URISyntaxException {

        mDefaultClientKey = key;
        final long initializationStartTime = System.currentTimeMillis();
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

        int factoryCount = mFactoryMonitor.count(apiToken);
        if (factoryCount > 0) {
            validationLogger.w("You already have " + factoryCount + (factoryCount == 1 ? " factory" : " factories") + "with this API Key. We recommend keeping only " +
                    "one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        } else if (mFactoryMonitor.count() > 0) {
            validationLogger.w("You already have an instance of the Split factory. Make sure you definitely want this additional instance. We recommend " +
                    "keeping only one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        }
        mFactoryMonitor.add(apiToken);
        mApiKey = apiToken;

        // Check if test database available
        String databaseName = factoryHelper.getDatabaseName(config, apiToken, context);
        SplitRoomDatabase _splitDatabase;
        if (testDatabase == null) {
            _splitDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        } else {
            _splitDatabase = testDatabase;
            Logger.d("Using test database");
        }

        defaultHttpClient.addHeaders(factoryHelper.buildHeaders(config, apiToken));
        defaultHttpClient.addStreamingHeaders(factoryHelper.buildStreamingHeaders(apiToken));

        mStorageContainer = factoryHelper.buildStorageContainer(_splitDatabase, key, config.shouldRecordTelemetry());

        SplitTaskExecutor _splitTaskExecutor = new SplitTaskExecutorImpl();

        String splitsFilterQueryString = factoryHelper.buildSplitsFilterQueryString(config);
        SplitApiFacade splitApiFacade = factoryHelper.buildApiFacade(
                config, defaultHttpClient, splitsFilterQueryString);

        EventsManagerCoordinator mEventsManagerCoordinator = new EventsManagerCoordinator();

        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(
                config, splitApiFacade, mStorageContainer, splitsFilterQueryString, mEventsManagerCoordinator);

        cleanUpDabase(_splitTaskExecutor, splitTaskFactory);

        Synchronizer mSynchronizer = new SynchronizerImpl(
                config,
                _splitTaskExecutor,
                mStorageContainer,
                splitTaskFactory,
                mEventsManagerCoordinator,
                factoryHelper.buildWorkManagerWrapper(
                        context, config, apiToken, key.matchingKey(), databaseName),
                new RetryBackoffCounterTimerFactory(),
                mStorageContainer.getTelemetryStorage());

        // Only available for integration tests
        if (synchronizerSpy != null) {
            synchronizerSpy.setSynchronizer(mSynchronizer);
            mSynchronizer = synchronizerSpy;
        }

        TelemetrySynchronizer telemetrySynchronizer = factoryHelper.getTelemetrySynchronizer(_splitTaskExecutor,
                splitTaskFactory,
                config.telemetryRefreshRate(),
                config.shouldRecordTelemetry());

        BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue
                = new LinkedBlockingDeque<>();

        NotificationParser notificationParser = new NotificationParser();
        NotificationProcessor notificationProcessor =
                new NotificationProcessor(_splitTaskExecutor, splitTaskFactory,
                        notificationParser, splitsUpdateNotificationQueue);

        PushManagerEventBroadcaster pushManagerEventBroadcaster = new PushManagerEventBroadcaster();

        SseClient sseClient = factoryHelper.getSseClient(config.streamingServiceUrl(),
                notificationParser,
                notificationProcessor,
                mStorageContainer.getTelemetryStorage(),
                pushManagerEventBroadcaster,
                defaultHttpClient);

        SseAuthenticator sseAuthenticator = new SseAuthenticator(splitApiFacade.getSseAuthenticationFetcher(),
                new SseJwtParser());

        PushNotificationManager pushNotificationManager = factoryHelper.getPushNotificationManager(_splitTaskExecutor,
                sseAuthenticator,
                pushManagerEventBroadcaster,
                sseClient,
                mStorageContainer.getTelemetryStorage());

        mSyncManager = factoryHelper.buildSyncManager(
                config,
                _splitTaskExecutor,
                mSynchronizer,
                telemetrySynchronizer,
                pushNotificationManager,
                splitsUpdateNotificationQueue,
                pushManagerEventBroadcaster
        );

        final ImpressionListener splitImpressionListener
                = new SyncImpressionListener(mSyncManager);
        final ImpressionListener customerImpressionListener;

        if (config.impressionListener() != null) {
            List<ImpressionListener> impressionListeners = new ArrayList<>();
            impressionListeners.add(splitImpressionListener);
            impressionListeners.add(config.impressionListener());
            customerImpressionListener = new ImpressionListener.FederatedImpressionListener(impressionListeners);
        } else {
            customerImpressionListener = splitImpressionListener;
        }

        mLifecycleManager = new SplitLifecycleManager();
        mLifecycleManager.register(mSyncManager);

        mDestroyer = new Runnable() {
            public void run() {
                Logger.w("Shutdown called for split");
                try {
                    mStorageContainer.getTelemetryStorage().recordSessionLength(System.currentTimeMillis() - initializationStartTime);
                    telemetrySynchronizer.flush();
                    telemetrySynchronizer.destroy();
                    Logger.i("Successful shutdown of telemetry");
                    mSyncManager.stop();
                    Logger.i("Flushing impressions and events");
                    mLifecycleManager.destroy();
                    Logger.i("Successful shutdown of lifecycle manager");
                    mFactoryMonitor.remove(mApiKey);
                    Logger.i("Successful shutdown of segment fetchers");
                    customerImpressionListener.close();
                    Logger.i("Successful shutdown of ImpressionListener");
                    defaultHttpClient.close();
                    Logger.i("Successful shutdown of httpclient");
                    mManager.destroy();
                    Logger.i("Successful shutdown of manager");
                    _splitTaskExecutor.stop();
                    Logger.i("Successful shutdown of task executor");
                    mStorageContainer.getAttributesStorageContainer().destroy();
                    Logger.i("Successful shutdown of attributes storage");
                } catch (Exception e) {
                    Logger.e(e, "We could not shutdown split");
                } finally {
                    mIsTerminated = true;
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

        SplitParser mSplitParser = new SplitParser(mStorageContainer.getMySegmentsStorageContainer());

        mClientContainer = new SplitClientContainerImpl(
                mDefaultClientKey.matchingKey(),
                this,
                config,
                mSyncManager,
                mSynchronizer,
                telemetrySynchronizer,
                mEventsManagerCoordinator,
                mStorageContainer,
                _splitTaskExecutor,
                splitApiFacade,
                validationLogger,
                keyValidator,
                customerImpressionListener,
                sseAuthenticator,
                pushNotificationManager);

        // Initialize default client
        client();

        mManager = new SplitManagerImpl(
                mStorageContainer.getSplitsStorage(),
                new SplitValidatorImpl(), mSplitParser);

        mSyncManager.start();

        if (config.shouldRecordTelemetry()) {
            int activeFactoriesCount = mFactoryMonitor.count(mApiKey);
            mStorageContainer.getTelemetryStorage().recordActiveFactories(activeFactoriesCount);
            mStorageContainer.getTelemetryStorage().recordRedundantFactories(activeFactoriesCount - 1);
        }

        Logger.i("Android SDK initialized!");
    }

    @Override
    public SplitClient client() {
        return client(mDefaultClientKey);
    }

    @Override
    public SplitClient client(Key key) {
        return mClientContainer.getClient(key);
    }

    @Override
    public SplitManager manager() {
        return mManager;
    }

    @Override
    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (mClientContainer.getAll().isEmpty() && !mIsTerminated) {
                new Thread(mDestroyer).start();
            }
        }
    }

    @Override
    public void flush() {
        mSyncManager.flush();
    }

    @Override
    public boolean isReady() {
        Set<SplitClient> clients = mClientContainer.getAll();
        for (SplitClient client : clients) {
            if (client.isReady()) {
                return true;
            }
        }

        return false;
    }

    private void setupValidations(SplitClientConfig splitClientConfig) {

        ValidationConfig.getInstance().setMaximumKeyLength(splitClientConfig.maximumKeyLength());
        ValidationConfig.getInstance().setTrackEventNamePattern(splitClientConfig.trackEventNamePattern());
    }

    private void cleanUpDabase(SplitTaskExecutor splitTaskExecutor,
                               SplitTaskFactory splitTaskFactory) {
        splitTaskExecutor.submit(splitTaskFactory.createCleanUpDatabaseTask(System.currentTimeMillis() / 1000), null);
    }
}
