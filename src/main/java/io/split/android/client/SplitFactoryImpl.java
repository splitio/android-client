package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.api.Key;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.DecoratedImpressionListener;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.SyncImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.lifecycle.SplitLifecycleManagerImpl;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.impressions.StrategyImpressionManager;
import io.split.android.client.service.impressions.strategy.ImpressionStrategyProvider;
import io.split.android.client.service.impressions.strategy.PeriodicTracker;
import io.split.android.client.service.impressions.strategy.ProcessStrategy;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.TargetingRulesCache;
import io.split.android.client.service.sseclient.sseclient.StreamingComponents;
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
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.splits.SplitsStorage;
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
    private final AtomicBoolean mIsTerminated = new AtomicBoolean(false);
    private final AtomicBoolean mCheckClients = new AtomicBoolean(false);
    private final String mApiKey;

    private final FactoryMonitor mFactoryMonitor = FactoryMonitorImpl.getSharedInstance();
    private final SplitLifecycleManager mLifecycleManager;
    private final SyncManager mSyncManager;

    private final SplitStorageContainer mStorageContainer;
    private final SplitClientContainer mClientContainer;
    private volatile UserConsentManager mUserConsentManager;
    private final ReentrantLock mInitLock = new ReentrantLock();

    private final EventsTrackerProvider mEventsTrackerProvider;
    private final StrategyImpressionManager mImpressionManager;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitClientConfig mConfig;

    private TargetingRulesCache mTargetingRulesCache = null;

    private final ExecutorService mInitExecutor = Executors.newFixedThreadPool(2);
    private static final Object INIT_LOCK = new Object();

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

        Pair<Map<SplitFilter.Type, SplitFilter>, String> filtersConfig = factoryHelper.getFilterConfiguration(config.syncConfig());
        Map<SplitFilter.Type, SplitFilter> filters = filtersConfig.first;
        String splitsFilterQueryStringFromConfig = filtersConfig.second;
        String flagsSpec = getFlagsSpec(testingConfig);
        FlagSetsFilter flagSetsFilter = factoryHelper.getFlagSetsFilter(filters);
        String databaseName = SplitFactoryHelper.buildDatabaseName(config, apiToken);

        // Check if this is a fresh install (no database exists and not using test database)
        WorkManagerWrapper workManagerWrapper = null;
        HttpClient defaultHttpClient = null;
        SplitApiFacade splitApiFacade = null;

        // Locked for concurrent factory inits
        synchronized (INIT_LOCK) {
            File dbPath = context.getDatabasePath(databaseName);
            if (!dbPath.exists() && testDatabase == null) {
                workManagerWrapper = factoryHelper.buildWorkManagerWrapper(context, config, apiToken, databaseName, filters);
                defaultHttpClient = getHttpClient(apiToken, config, context, httpClient, workManagerWrapper, factoryHelper, null);
                splitApiFacade = factoryHelper.buildApiFacade(config, defaultHttpClient, splitsFilterQueryStringFromConfig);
                startFreshInstallPrefetch(splitApiFacade, flagsSpec, initializationStartTime);
            }
        }

        SplitRoomDatabase splitDatabase;
        if (testDatabase == null) {
            splitDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        } else {
            splitDatabase = testDatabase;
            Logger.d("Using test database");
        }
        mConfig = config;
        SplitCipher splitCipher = factoryHelper.getCipher(apiToken, config.encryptionEnabled());

        // At the moment this cipher is only used for proxy config
        SplitCipher alwaysEncryptedSplitCipher = (config.synchronizeInBackground() && config.proxy() != null && !config.proxy().isLegacy()) ?
                factoryHelper.getCipher(apiToken, true) : null;

        SplitsStorage splitsStorage = StorageFactory.getSplitsStorage(splitDatabase, splitCipher);

        ScheduledThreadPoolExecutor impressionsObserverExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadPoolExecutor.CallerRunsPolicy());

        mStorageContainer = factoryHelper.buildStorageContainer(config.userConsent(),
                splitDatabase, config.shouldRecordTelemetry(), splitCipher, telemetryStorage, config.observerCacheExpirationPeriod(), impressionsObserverExecutor, splitsStorage, alwaysEncryptedSplitCipher);

        mSplitTaskExecutor = new SplitTaskExecutorImpl();
        mSplitTaskExecutor.pause();

        EventsManagerCoordinator mEventsManagerCoordinator = new EventsManagerCoordinator();

        // Build WorkManager and Api Facade in case they weren't present
        if (workManagerWrapper == null) {
            workManagerWrapper = factoryHelper.buildWorkManagerWrapper(context, config, apiToken, databaseName, filters);
        }
        if (defaultHttpClient == null) {
            defaultHttpClient = getHttpClient(apiToken, config, context, httpClient, workManagerWrapper, factoryHelper, mStorageContainer.getGeneralInfoStorage());
        }
        if (splitApiFacade == null) {
            splitApiFacade = factoryHelper.buildApiFacade(config, defaultHttpClient, splitsFilterQueryStringFromConfig);
        }

        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(
                config, splitApiFacade, mStorageContainer, splitsFilterQueryStringFromConfig,
                getFlagsSpec(testingConfig), mEventsManagerCoordinator, filters, flagSetsFilter,
                testingConfig, mTargetingRulesCache);

        SplitSingleThreadTaskExecutor splitSingleThreadTaskExecutor = new SplitSingleThreadTaskExecutor();
        splitSingleThreadTaskExecutor.pause();

        ImpressionStrategyProvider impressionStrategyProvider = factoryHelper.getImpressionStrategyProvider(mSplitTaskExecutor, splitTaskFactory, mStorageContainer, config);
        Pair<ProcessStrategy, PeriodicTracker> noneComponents = impressionStrategyProvider.getNoneComponents();

        mImpressionManager = new StrategyImpressionManager(noneComponents, impressionStrategyProvider.getStrategy(config.impressionsMode()));
        final RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory = new RetryBackoffCounterTimerFactory();

        StreamingComponents streamingComponents = factoryHelper.buildStreamingComponents(mSplitTaskExecutor,
                splitTaskFactory, config, defaultHttpClient, splitApiFacade, mStorageContainer, flagsSpec);

        Synchronizer mSynchronizer = new SynchronizerImpl(
                config,
                mSplitTaskExecutor,
                splitSingleThreadTaskExecutor,
                splitTaskFactory,
                workManagerWrapper,
                retryBackoffCounterTimerFactory,
                mStorageContainer.getTelemetryStorage(),
                new AttributesSynchronizerRegistryImpl(),
                new MySegmentsSynchronizerRegistryImpl(),
                mImpressionManager,
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

        TelemetrySynchronizer telemetrySynchronizer = factoryHelper.getTelemetrySynchronizer(mSplitTaskExecutor,
                splitTaskFactory, config.telemetryRefreshRate(), config.shouldRecordTelemetry());

        mSyncManager = factoryHelper.buildSyncManager(
                config,
                mSplitTaskExecutor,
                mSynchronizer,
                telemetrySynchronizer,
                streamingComponents.getPushNotificationManager(),
                streamingComponents.getPushManagerEventBroadcaster(),
                factoryHelper.getSplitUpdatesWorker(config,
                        mSplitTaskExecutor,
                        splitTaskFactory,
                        mSynchronizer,
                        streamingComponents.getSplitsUpdateNotificationQueue(),
                        mStorageContainer.getSplitsStorage(),
                        mStorageContainer.getRuleBasedSegmentStorage(),
                        compressionProvider),
                streamingComponents.getSyncGuardian());

        if (testLifecycleManager == null) {
            mLifecycleManager = new SplitLifecycleManagerImpl();
        } else {
            mLifecycleManager = testLifecycleManager;
        }

        ExecutorService impressionsLoggingTaskExecutor = factoryHelper.getImpressionsLoggingTaskExecutor();
        final DecoratedImpressionListener splitImpressionListener
                = new SyncImpressionListener(mSyncManager, impressionsLoggingTaskExecutor);
        final ImpressionListener.FederatedImpressionListener customerImpressionListener;

        List<ImpressionListener> impressionListeners = new ArrayList<>();
        if (config.impressionListener() != null) {
            impressionListeners.add(config.impressionListener());
            customerImpressionListener = new ImpressionListener.FederatedImpressionListener(splitImpressionListener, impressionListeners);
        } else {
            customerImpressionListener = new ImpressionListener.FederatedImpressionListener(splitImpressionListener, impressionListeners);
        }

        mEventsTrackerProvider = new EventsTrackerProvider(mStorageContainer.getSplitsStorage(),
                mStorageContainer.getTelemetryStorage(), mSyncManager);

        ClientComponentsRegister componentsRegister = factoryHelper.getClientComponentsRegister(config, mSplitTaskExecutor,
                mEventsManagerCoordinator, mSynchronizer, streamingComponents.getNotificationParser(),
                streamingComponents.getNotificationProcessor(), streamingComponents.getSseAuthenticator(),
                mStorageContainer, mSyncManager, compressionProvider);

        SplitParser splitParser = new SplitParser(mStorageContainer.getParserCommons());

        mClientContainer = new SplitClientContainerImpl(
                mDefaultClientKey.matchingKey(), this, config, mSyncManager,
                telemetrySynchronizer, mStorageContainer, mSplitTaskExecutor, splitApiFacade,
                validationLogger, keyValidator, customerImpressionListener,
                streamingComponents.getPushNotificationManager(), componentsRegister, workManagerWrapper,
                mEventsTrackerProvider, flagSetsFilter, splitParser);

        // Set up async initialization
        final SplitFactoryHelper.Initializer initializer = new SplitFactoryHelper.Initializer(apiToken,
                config,
                splitTaskFactory,
                splitDatabase,
                splitCipher,
                mEventsManagerCoordinator,
                mSplitTaskExecutor,
                splitSingleThreadTaskExecutor,
                mStorageContainer,
                mSyncManager,
                mLifecycleManager,
                mInitLock);

        if (config.shouldRecordTelemetry()) {
            int activeFactoriesCount = mFactoryMonitor.count(mApiKey);
            mStorageContainer.getTelemetryStorage().recordActiveFactories(activeFactoriesCount);
            mStorageContainer.getTelemetryStorage().recordRedundantFactories(activeFactoriesCount - 1);
        }

        // Run initializer
        mInitExecutor.submit(initializer);

        CleanUpDatabaseTask cleanUpDatabaseTask = splitTaskFactory.createCleanUpDatabaseTask(System.currentTimeMillis() / 1000);
        mSplitTaskExecutor.schedule(cleanUpDatabaseTask, 5L, null);

        // Initialize default client
        client();
        mManager = new SplitManagerImpl(
                mStorageContainer.getSplitsStorage(),
                new SplitValidatorImpl(), splitParser);
        mDestroyer = new Destroyer(
                mInitLock,
                mCheckClients,
                mClientContainer,
                mStorageContainer,
                initializationStartTime,
                telemetrySynchronizer,
                impressionsLoggingTaskExecutor,
                impressionsObserverExecutor,
                mSyncManager,
                mLifecycleManager,
                mFactoryMonitor,
                mApiKey,
                customerImpressionListener,
                defaultHttpClient,
                mManager,
                mSplitTaskExecutor,
                splitSingleThreadTaskExecutor,
                mInitExecutor,
                mIsTerminated);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Using the full path to avoid conflicting with Thread.destroy()
                SplitFactoryImpl.this.destroy();
            }
        });
    }

    @NonNull
    private static HttpClient getHttpClient(@NonNull String apiToken,
                                            @NonNull SplitClientConfig config,
                                            @NonNull Context context,
                                            @Nullable HttpClient httpClient,
                                            WorkManagerWrapper workManagerWrapper,
                                            SplitFactoryHelper factoryHelper,
                                            @Nullable GeneralInfoStorage generalInfoStorage) {
        HttpClient defaultHttpClient;
        if (httpClient == null) {
            HttpClientImpl.Builder builder = new HttpClientImpl.Builder()
                    .setConnectionTimeout(config.connectionTimeout())
                    .setReadTimeout(config.readTimeout())
                    .setDevelopmentSslConfig(config.developmentSslConfig())
                    .setContext(context)
                    .setProxyAuthenticator(config.authenticator());
            if (config.proxy() != null) {
                builder.setProxy(config.proxy());
            }
            if (config.certificatePinningConfiguration() != null) {
                builder.setCertificatePinningConfiguration(config.certificatePinningConfiguration());
            }

            defaultHttpClient = builder.build();

            // This should be extracted; has nothing to do with the method.
            if (config.proxy() != null && generalInfoStorage != null) {
                SplitFactoryHelper.setupProxyForBackgroundSync(config,
                        SplitFactoryHelper.getProxyConfigSaveTask(config,
                                workManagerWrapper,
                                generalInfoStorage));
            }
        } else {
            defaultHttpClient = httpClient;
        }
        defaultHttpClient.addHeaders(factoryHelper.buildHeaders(config, apiToken));
        defaultHttpClient.addStreamingHeaders(factoryHelper.buildStreamingHeaders(apiToken));
        return defaultHttpClient;
    }

    private static String getFlagsSpec(@Nullable TestingConfig testingConfig) {
        if (testingConfig == null) {
            return BuildConfig.FLAGS_SPEC;
        } else {
            return testingConfig.getFlagsSpec();
        }
    }

    private void startFreshInstallPrefetch(@NonNull SplitApiFacade splitApiFacade, @NonNull String flagsSpec, long initializationStartTime) {
        mTargetingRulesCache = new TargetingRulesCache();

        Runnable prefetch = () -> {
            try {
                Logger.d("Fresh install detected - prefetching targeting rules");
                SplitsSyncHelper.fetchSplits(
                        new SplitsSyncHelper.SinceChangeNumbers(-1L, -1L),
                        true,
                        flagsSpec,
                        splitApiFacade.getSplitFetcher(),
                        mTargetingRulesCache);
                long elapsedTime = System.currentTimeMillis() - initializationStartTime;
                Logger.d("Fresh install prefetch completed in " + elapsedTime + "ms");
            } catch (HttpFetcherException e) {
                Logger.w("Error prefetching targeting rules on fresh install: " + e.getLocalizedMessage());
            }
        };
        mInitExecutor.submit(prefetch);
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
            if (!mIsTerminated.get()) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.schedule(mDestroyer, 100, TimeUnit.MILLISECONDS);
                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        executor.shutdown();
                        try {
                            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                                executor.shutdownNow();
                            }
                        } catch (InterruptedException e) {
                            executor.shutdownNow();
                            Thread.currentThread().interrupt();
                        }
                    }
                }, 500, TimeUnit.MILLISECONDS);
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
        if (getUserConsentManager() == null) {
            Logger.e("User consent manager not initialized. Unable to set mode " + newMode.toString());
            return;
        }
        getUserConsentManager().setStatus(newMode);
    }

    private UserConsentManager getUserConsentManager() {
        if (mUserConsentManager == null) {
            synchronized (mConfig) {
                if (mUserConsentManager == null) {
                    mUserConsentManager = new UserConsentManagerImpl(mConfig,
                            mStorageContainer.getImpressionsStorage(),
                            mStorageContainer.getEventsStorage(),
                            mSyncManager, mEventsTrackerProvider, mImpressionManager, mSplitTaskExecutor);
                }
            }
        }

        return mUserConsentManager;
    }

    @Override
    public UserConsent getUserConsent() {
        return getUserConsentManager().getStatus();
    }

    void checkClients() {
        mCheckClients.set(true);
    }

    private void setupValidations(SplitClientConfig splitClientConfig) {
        ValidationConfig.getInstance().setMaximumKeyLength(splitClientConfig.maximumKeyLength());
        ValidationConfig.getInstance().setTrackEventNamePattern(splitClientConfig.trackEventNamePattern());
    }

    public static class EventsTrackerProvider {

        private final SplitsStorage mSplitsStorage;
        private final TelemetryStorage mTelemetryStorage;
        private final SyncManager mSyncManager;
        private volatile EventsTracker mEventsTracker;

        public EventsTrackerProvider(SplitsStorage splitsStorage, TelemetryStorage telemetryStorage, SyncManager syncManager) {
            mSplitsStorage = splitsStorage;
            mTelemetryStorage = telemetryStorage;
            mSyncManager = syncManager;
        }

        public EventsTracker getEventsTracker() {
            if (mEventsTracker == null) {
                synchronized (this) {
                    if (mEventsTracker == null) {
                        EventValidator eventsValidator = new EventValidatorImpl(new KeyValidatorImpl(), mSplitsStorage);
                        mEventsTracker = new EventsTrackerImpl(eventsValidator, new ValidationMessageLoggerImpl(), mTelemetryStorage,
                                new PropertyValidatorImpl(), mSyncManager);
                    }
                }
            }

            return mEventsTracker;
        }
    }
}
