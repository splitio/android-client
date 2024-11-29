package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.api.Key;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.SyncImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.lifecycle.SplitLifecycleManagerImpl;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.impressions.StrategyImpressionManager;
import io.split.android.client.service.sseclient.sseclient.StreamingComponents;
import io.split.android.client.service.synchronizer.RolloutCacheManagerImpl;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistryImpl;
import io.split.android.client.shared.ClientComponentsRegister;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.shared.SplitClientContainerImpl;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.cipher.EncryptionMigrationTask;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.ApiKeyValidator;
import io.split.android.client.validators.ApiKeyValidatorImpl;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.EventValidatorImpl;
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
    private final UserConsentManager mUserConsentManager;

    public SplitFactoryImpl(@NonNull String apiToken, @NonNull Key key, @NonNull SplitClientConfig config, @NonNull Context context)
            throws URISyntaxException {
        this(apiToken, key, config, context,
                null, null, null,
                null, null, null);
    }

    private SplitFactoryImpl(@NonNull String apiToken, @NonNull Key key, @NonNull SplitClientConfig config,
                             @NonNull Context context, @Nullable HttpClient httpClient, @Nullable SplitRoomDatabase testDatabase,
                             @Nullable SynchronizerSpy synchronizerSpy,
                             @Nullable TestingConfig testingConfig, @Nullable SplitLifecycleManager testLifecycleManager,
                             @Nullable TelemetryStorage telemetryStorage)
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
            HttpClientImpl.Builder builder = new HttpClientImpl.Builder()
                    .setConnectionTimeout(config.connectionTimeout())
                    .setReadTimeout(config.readTimeout())
                    .setProxy(config.proxy())
                    .setDevelopmentSslConfig(config.developmentSslConfig())
                    .setContext(context)
                    .setProxyAuthenticator(config.authenticator());
            if (config.certificatePinningConfiguration() != null) {
                builder.setCertificatePinningConfiguration(config.certificatePinningConfiguration());
            }

            defaultHttpClient = builder.build();
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
            validationLogger.w("You already have " + factoryCount + (factoryCount == 1 ? " factory" : " factories") + " with this SDK Key. We recommend keeping only " +
                    "one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        } else if (mFactoryMonitor.count() > 0) {
            validationLogger.w("You already have an instance of the Split factory. Make sure you definitely want this additional instance. We recommend " +
                    "keeping only one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        }
        mFactoryMonitor.add(apiToken);
        mApiKey = apiToken;

        // Check if test database available
        String databaseName = factoryHelper.getDatabaseName(config, apiToken, context);
        SplitRoomDatabase splitDatabase;
        if (testDatabase == null) {
            splitDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        } else {
            splitDatabase = testDatabase;
            Logger.d("Using test database");
        }

        defaultHttpClient.addHeaders(factoryHelper.buildHeaders(config, apiToken));
        defaultHttpClient.addStreamingHeaders(factoryHelper.buildStreamingHeaders(apiToken));

        SplitTaskExecutor splitTaskExecutor = new SplitTaskExecutorImpl();
        splitTaskExecutor.pause();

        EventsManagerCoordinator mEventsManagerCoordinator = new EventsManagerCoordinator();

        SplitCipher splitCipher = factoryHelper.getCipher(apiToken, config.encryptionEnabled());

        ScheduledThreadPoolExecutor impressionsObserverExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadPoolExecutor.CallerRunsPolicy());
        mStorageContainer = factoryHelper.buildStorageContainer(config.userConsent(),
                splitDatabase, config.shouldRecordTelemetry(), splitCipher, telemetryStorage, config.observerCacheExpirationPeriod(), impressionsObserverExecutor);

        Pair<Map<SplitFilter.Type, SplitFilter>, String> filtersConfig = factoryHelper.getFilterConfiguration(config.syncConfig());
        Map<SplitFilter.Type, SplitFilter> filters = filtersConfig.first;
        String splitsFilterQueryStringFromConfig = filtersConfig.second;

        String flagsSpec = getFlagsSpec(testingConfig);
        SplitApiFacade splitApiFacade = factoryHelper.buildApiFacade(
                config, defaultHttpClient, splitsFilterQueryStringFromConfig);

        FlagSetsFilter flagSetsFilter = factoryHelper.getFlagSetsFilter(filters);

        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(
                config, splitApiFacade, mStorageContainer, splitsFilterQueryStringFromConfig,
                getFlagsSpec(testingConfig), mEventsManagerCoordinator, filters, flagSetsFilter, testingConfig);

        cleanUpDabase(splitTaskExecutor, splitTaskFactory);
        WorkManagerWrapper workManagerWrapper = factoryHelper.buildWorkManagerWrapper(context, config, apiToken, databaseName, filters);
        SplitSingleThreadTaskExecutor splitSingleThreadTaskExecutor = new SplitSingleThreadTaskExecutor();
        splitSingleThreadTaskExecutor.pause();

        ImpressionManager impressionManager = new StrategyImpressionManager(factoryHelper.getImpressionStrategy(splitTaskExecutor, splitTaskFactory, mStorageContainer, config));
        final RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory = new RetryBackoffCounterTimerFactory();

        StreamingComponents streamingComponents = factoryHelper.buildStreamingComponents(splitTaskExecutor,
                splitTaskFactory, config, defaultHttpClient, splitApiFacade, mStorageContainer, flagsSpec);
        Synchronizer mSynchronizer = new SynchronizerImpl(
                config,
                splitTaskExecutor,
                splitSingleThreadTaskExecutor,
                splitTaskFactory,
                workManagerWrapper,
                retryBackoffCounterTimerFactory,
                mStorageContainer.getTelemetryStorage(),
                new AttributesSynchronizerRegistryImpl(),
                new MySegmentsSynchronizerRegistryImpl(),
                impressionManager,
                mStorageContainer.getEventsStorage(),
                mEventsManagerCoordinator,
                streamingComponents.getPushManagerEventBroadcaster()
        );
        // Only available for integration tests
        if (synchronizerSpy != null) {
            synchronizerSpy.setSynchronizer(mSynchronizer);
            mSynchronizer = synchronizerSpy;
        }

        CompressionUtilProvider compressionProvider = new CompressionUtilProvider();

        TelemetrySynchronizer telemetrySynchronizer = factoryHelper.getTelemetrySynchronizer(splitTaskExecutor,
                splitTaskFactory, config.telemetryRefreshRate(), config.shouldRecordTelemetry());

        mSyncManager = factoryHelper.buildSyncManager(
                config,
                splitTaskExecutor,
                mSynchronizer,
                telemetrySynchronizer,
                streamingComponents.getPushNotificationManager(),
                streamingComponents.getPushManagerEventBroadcaster(),
                factoryHelper.getSplitUpdatesWorker(config,
                        splitTaskExecutor,
                        splitTaskFactory,
                        mSynchronizer,
                        streamingComponents.getSplitsUpdateNotificationQueue(),
                        mStorageContainer.getSplitsStorage(),
                        compressionProvider),
                streamingComponents.getSyncGuardian());

        if (testLifecycleManager == null) {
            mLifecycleManager = new SplitLifecycleManagerImpl();
        } else {
            mLifecycleManager = testLifecycleManager;
        }

        ExecutorService impressionsLoggingTaskExecutor = factoryHelper.getImpressionsLoggingTaskExecutor();
        final ImpressionListener splitImpressionListener
                = new SyncImpressionListener(mSyncManager, impressionsLoggingTaskExecutor);
        final ImpressionListener customerImpressionListener;

        if (config.impressionListener() != null) {
            List<ImpressionListener> impressionListeners = new ArrayList<>();
            impressionListeners.add(splitImpressionListener);
            impressionListeners.add(config.impressionListener());
            customerImpressionListener = new ImpressionListener.FederatedImpressionListener(impressionListeners);
        } else {
            customerImpressionListener = splitImpressionListener;
        }
        EventsTracker eventsTracker = buildEventsTracker();
        mUserConsentManager = new UserConsentManagerImpl(config,
                mStorageContainer.getImpressionsStorage(),
                mStorageContainer.getEventsStorage(),
                mSyncManager, eventsTracker, impressionManager, splitTaskExecutor);

        ClientComponentsRegister componentsRegister = factoryHelper.getClientComponentsRegister(config, splitTaskExecutor,
                mEventsManagerCoordinator, mSynchronizer, streamingComponents.getNotificationParser(),
                streamingComponents.getNotificationProcessor(), streamingComponents.getSseAuthenticator(),
                mStorageContainer, mSyncManager, compressionProvider);
        mClientContainer = new SplitClientContainerImpl(
                mDefaultClientKey.matchingKey(), this, config, mSyncManager,
                telemetrySynchronizer, mStorageContainer, splitTaskExecutor, splitApiFacade,
                validationLogger, keyValidator, customerImpressionListener,
                streamingComponents.getPushNotificationManager(), componentsRegister, workManagerWrapper,
                eventsTracker, flagSetsFilter);
        mDestroyer = new Runnable() {
            public void run() {
                Logger.w("Shutdown called for split");
                try {
                    mStorageContainer.getTelemetryStorage().recordSessionLength(System.currentTimeMillis() - initializationStartTime);
                    telemetrySynchronizer.flush();
                    telemetrySynchronizer.destroy();
                    Logger.d("Successful shutdown of telemetry");
                    impressionsLoggingTaskExecutor.shutdown();
                    impressionsObserverExecutor.shutdown();
                    Logger.d("Successful shutdown of impressions logging executor");
                    mSyncManager.stop();
                    Logger.d("Flushing impressions and events");
                    mLifecycleManager.destroy();
                    Logger.d("Successful shutdown of lifecycle manager");
                    mFactoryMonitor.remove(mApiKey);
                    Logger.d("Successful shutdown of segment fetchers");
                    customerImpressionListener.close();
                    Logger.d("Successful shutdown of ImpressionListener");
                    defaultHttpClient.close();
                    Logger.d("Successful shutdown of httpclient");
                    mManager.destroy();
                    Logger.d("Successful shutdown of manager");
                    splitTaskExecutor.stop();
                    splitSingleThreadTaskExecutor.stop();
                    Logger.d("Successful shutdown of task executor");
                    mStorageContainer.getAttributesStorageContainer().destroy();
                    Logger.d("Successful shutdown of attributes storage");
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

        // Initialize default client
        client();
        SplitParser mSplitParser = new SplitParser(mStorageContainer.getMySegmentsStorageContainer(), mStorageContainer.getMyLargeSegmentsStorageContainer());
        mManager = new SplitManagerImpl(
                mStorageContainer.getSplitsStorage(),
                new SplitValidatorImpl(), mSplitParser);

        Runnable initializer = new Runnable() {
            @Override
            public void run() {
                Logger.v("Running initialization thread");
                RolloutCacheManagerImpl rolloutCacheManager = new RolloutCacheManagerImpl(config, mStorageContainer, new EncryptionMigrationTask(apiToken, splitDatabase, config.encryptionEnabled(), splitCipher));
                rolloutCacheManager.validateCache(new SplitTaskExecutionListener() {
                    @Override
                    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                        mEventsManagerCoordinator.notifyInternalEvent(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
                        splitTaskExecutor.resume();
                        splitSingleThreadTaskExecutor.resume();
                        mSyncManager.start();
                        mLifecycleManager.register(mSyncManager);

                        Logger.i("Android SDK initialized!");
                    }
                });
            }
        };
        new Thread(initializer).start();

        if (config.shouldRecordTelemetry()) {
            int activeFactoriesCount = mFactoryMonitor.count(mApiKey);
            mStorageContainer.getTelemetryStorage().recordActiveFactories(activeFactoriesCount);
            mStorageContainer.getTelemetryStorage().recordRedundantFactories(activeFactoriesCount - 1);
        }
    }

    private static String getFlagsSpec(@Nullable TestingConfig testingConfig) {
        if (testingConfig == null) {
            return BuildConfig.FLAGS_SPEC;
        } else {
            return testingConfig.getFlagsSpec();
        }
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
    public SplitClient client(String matchingKey) {
        return mClientContainer.getClient(new Key(matchingKey));
    }

    @Override
    public SplitClient client(String matchingKey, String bucketingKey) {
        return mClientContainer.getClient(new Key(matchingKey, bucketingKey));
    }

    @Override
    public SplitManager manager() {
        return mManager;
    }

    @Override
    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (!mIsTerminated) {
                new Thread(mDestroyer).start();
            }
        }
    }

    @Override
    public void flush() {
        mSyncManager.flush();
    }

    @Override
    public void setUserConsent(boolean enabled) {
        UserConsent newMode = (enabled ? UserConsent.GRANTED : UserConsent.DECLINED);
        if (mUserConsentManager == null) {
            Logger.e("User consent manager not initialized. Unable to set mode " + newMode.toString());
            return;
        }
        mUserConsentManager.setStatus(newMode);
    }

    @Override
    public UserConsent getUserConsent() {
        return mUserConsentManager.getStatus();
    }

    private void setupValidations(SplitClientConfig splitClientConfig) {

        ValidationConfig.getInstance().setMaximumKeyLength(splitClientConfig.maximumKeyLength());
        ValidationConfig.getInstance().setTrackEventNamePattern(splitClientConfig.trackEventNamePattern());
    }

    private void cleanUpDabase(SplitTaskExecutor splitTaskExecutor,
                               SplitTaskFactory splitTaskFactory) {
        splitTaskExecutor.submit(splitTaskFactory.createCleanUpDatabaseTask(System.currentTimeMillis() / 1000), null);
    }

    private EventsTracker buildEventsTracker() {
        EventValidator eventsValidator = new EventValidatorImpl(new KeyValidatorImpl(), mStorageContainer.getSplitsStorage());
        return new EventsTrackerImpl(eventsValidator, new ValidationMessageLoggerImpl(), mStorageContainer.getTelemetryStorage(),
                new EventPropertiesProcessorImpl(), mSyncManager);
    }
}
