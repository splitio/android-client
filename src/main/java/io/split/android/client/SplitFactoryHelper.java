package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.work.WorkManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.http.mysegments.MySegmentsFetcherFactoryImpl;
import io.split.android.client.service.impressions.strategy.ImpressionStrategyProvider;
import io.split.android.client.service.impressions.strategy.ProcessStrategy;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.MySegmentsPayloadDecoder;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorFactory;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorFactoryImpl;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.sseclient.sseclient.SseClientImpl;
import io.split.android.client.service.sseclient.sseclient.SseHandler;
import io.split.android.client.service.sseclient.sseclient.SseRefreshTokenTimer;
import io.split.android.client.service.sseclient.sseclient.StreamingComponents;
import io.split.android.client.service.synchronizer.SyncGuardian;
import io.split.android.client.service.synchronizer.SyncGuardianImpl;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactory;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.shared.ClientComponentsRegisterImpl;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.cipher.EncryptionMigrationTask;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.cipher.SplitEncryptionLevel;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.TelemetrySynchronizerImpl;
import io.split.android.client.telemetry.TelemetrySynchronizerStub;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.Utils;

class SplitFactoryHelper {
    private static final int DB_MAGIC_CHARS_COUNT = 4;

    String getDatabaseName(SplitClientConfig config, String apiToken, Context context) {

        String dbName = buildDatabaseName(config, apiToken);
        File dbPath = context.getDatabasePath(dbName);
        if (dbPath.exists()) {
            return dbName;
        }

        String legacyName = buildLegacyDatabaseName(config, apiToken);
        File legacyDb = context.getDatabasePath(legacyName);
        if (legacyDb.exists()) {
            legacyDb.renameTo(dbPath);
        }
        return dbName;
    }

    private String buildDatabaseName(SplitClientConfig config, String apiToken) {
        if (apiToken == null) {
            throw new IllegalArgumentException("SDK key cannot be null");
        }

        final int apiTokenLength = apiToken.length();
        final String prefix = (config.prefix() == null) ? "" : config.prefix();

        if (apiTokenLength > DB_MAGIC_CHARS_COUNT) {
            String begin = apiToken.substring(0, DB_MAGIC_CHARS_COUNT);
            String end = apiToken.substring(apiTokenLength - DB_MAGIC_CHARS_COUNT);
            return prefix + begin + end;
        }

        return prefix + config.defaultDataFolder();
    }

    private String buildLegacyDatabaseName(SplitClientConfig splitClientConfig, String apiToken) {
        String databaseName = Utils.convertApiKeyToFolder(apiToken);
        if (databaseName == null) {
            databaseName = splitClientConfig.defaultDataFolder();
        }
        return databaseName;
    }

    Map<String, String> buildHeaders(SplitClientConfig splitClientConfig, String apiToken) {
        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.addJsonTypeHeaders();
        headersBuilder.setHostIp(splitClientConfig.ip());
        headersBuilder.setHostname(splitClientConfig.hostname());
        headersBuilder.setClientVersion(SplitClientConfig.splitSdkVersion);
        headersBuilder.setApiToken(apiToken);
        return headersBuilder.build();
    }

    Map<String, String> buildStreamingHeaders(String apiToken) {
        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.addStreamingTypeHeaders();
        headersBuilder.setAblyApiToken(apiToken);
        headersBuilder.setClientVersion(SplitClientConfig.splitSdkVersion);
        return headersBuilder.build();
    }

    SplitStorageContainer buildStorageContainer(UserConsent userConsentStatus,
                                                SplitRoomDatabase splitRoomDatabase,
                                                boolean shouldRecordTelemetry,
                                                SplitCipher splitCipher,
                                                TelemetryStorage telemetryStorage,
                                                long observerCacheExpirationPeriod) {

        boolean isPersistenceEnabled = userConsentStatus == UserConsent.GRANTED;
        PersistentEventsStorage persistentEventsStorage =
                StorageFactory.getPersistentEventsStorage(splitRoomDatabase, splitCipher);
        PersistentImpressionsStorage persistentImpressionsStorage =
                StorageFactory.getPersistentImpressionsStorage(splitRoomDatabase, splitCipher);
        return new SplitStorageContainer(
                StorageFactory.getSplitsStorage(splitRoomDatabase, splitCipher),
                StorageFactory.getMySegmentsStorage(splitRoomDatabase, splitCipher),
                StorageFactory.getPersistentSplitsStorage(splitRoomDatabase, splitCipher),
                StorageFactory.getEventsStorage(persistentEventsStorage, isPersistenceEnabled),
                persistentEventsStorage,
                StorageFactory.getImpressionsStorage(persistentImpressionsStorage, isPersistenceEnabled),
                persistentImpressionsStorage,
                StorageFactory.getPersistentImpressionsCountStorage(splitRoomDatabase, splitCipher),
                StorageFactory.getPersistentImpressionsUniqueStorage(splitRoomDatabase, splitCipher),
                StorageFactory.getAttributesStorage(),
                StorageFactory.getPersistentAttributesStorage(splitRoomDatabase, splitCipher),
                getTelemetryStorage(shouldRecordTelemetry, telemetryStorage),
                StorageFactory.getImpressionsObserverCachePersistentStorage(splitRoomDatabase, observerCacheExpirationPeriod));
    }

    SplitApiFacade buildApiFacade(SplitClientConfig splitClientConfig,
                                  HttpClient httpClient,
                                  String splitsFilterQueryString) throws URISyntaxException {

        return new SplitApiFacade(
                ServiceFactory.getSplitsFetcher(httpClient,
                        splitClientConfig.endpoint(), splitsFilterQueryString),
                new MySegmentsFetcherFactoryImpl(httpClient,
                        splitClientConfig.endpoint()),
                ServiceFactory.getSseAuthenticationFetcher(httpClient,
                        splitClientConfig.authServiceUrl()),
                ServiceFactory.getEventsRecorder(httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getImpressionsRecorder(httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getImpressionsCountRecorder(httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getUniqueKeysRecorder(httpClient,
                        splitClientConfig.telemetryEndpoint()),
                ServiceFactory.getTelemetryConfigRecorder(httpClient,
                        splitClientConfig.telemetryEndpoint()),
                ServiceFactory.getTelemetryStatsRecorder(httpClient,
                        splitClientConfig.telemetryEndpoint()));
    }

    WorkManagerWrapper buildWorkManagerWrapper(Context context, SplitClientConfig splitClientConfig,
                                               String apiKey, String databaseName, Map<SplitFilter.Type, SplitFilter> filters) {
        SplitFilter filter = filters.get(SplitFilter.Type.BY_SET) != null ?
                filters.get(SplitFilter.Type.BY_SET) :
                filters.get(SplitFilter.Type.BY_NAME);
        return new WorkManagerWrapper(
                WorkManager.getInstance(context), splitClientConfig, apiKey, databaseName, filter);

    }

    SyncManager buildSyncManager(SplitClientConfig config,
                                 SplitTaskExecutor splitTaskExecutor,
                                 Synchronizer synchronizer,
                                 TelemetrySynchronizer telemetrySynchronizer,
                                 @Nullable PushNotificationManager pushNotificationManager,
                                 @Nullable PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                 @Nullable SplitUpdatesWorker splitUpdatesWorker,
                                 @Nullable SyncGuardian syncGuardian) {


        BackoffCounterTimer backoffCounterTimer = null;
        if (config.syncEnabled()) {
            backoffCounterTimer = new BackoffCounterTimer(splitTaskExecutor, new ReconnectBackoffCounter(1));
        }

        return new SyncManagerImpl(config,
                synchronizer,
                pushNotificationManager,
                splitUpdatesWorker,
                pushManagerEventBroadcaster,
                backoffCounterTimer,
                syncGuardian,
                telemetrySynchronizer);
    }

    @NonNull
    PushNotificationManager getPushNotificationManager(SplitTaskExecutor splitTaskExecutor,
                                                       SseAuthenticator sseAuthenticator,
                                                       PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                                       SseClient sseClient,
                                                       TelemetryRuntimeProducer telemetryRuntimeProducer,
                                                       long defaultSseConnectionDelayInSecs,
                                                       long sseDisconnectionDelayInSecs) {
        return new PushNotificationManager(pushManagerEventBroadcaster,
                sseAuthenticator,
                sseClient,
                new SseRefreshTokenTimer(splitTaskExecutor, pushManagerEventBroadcaster),
                telemetryRuntimeProducer,
                defaultSseConnectionDelayInSecs,
                sseDisconnectionDelayInSecs,
                null);
    }

    public SseClient getSseClient(String streamingServiceUrlString,
                                  NotificationParser notificationParser,
                                  NotificationProcessor notificationProcessor,
                                  TelemetryRuntimeProducer telemetryRuntimeProducer,
                                  PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                  HttpClient httpClient) {
        SseHandler sseHandler = new SseHandler(notificationParser,
                notificationProcessor,
                telemetryRuntimeProducer,
                pushManagerEventBroadcaster);

        return new SseClientImpl(URI.create(streamingServiceUrlString),
                httpClient,
                new EventStreamParser(),
                sseHandler);
    }

    @NonNull
    TelemetrySynchronizer getTelemetrySynchronizer(SplitTaskExecutor _splitTaskExecutor,
                                                   SplitTaskFactory splitTaskFactory,
                                                   long telemetryRefreshRate,
                                                   boolean shouldRecordTelemetry) {
        if (shouldRecordTelemetry) {
            return new TelemetrySynchronizerImpl(_splitTaskExecutor, splitTaskFactory, telemetryRefreshRate);
        } else {
            return new TelemetrySynchronizerStub();
        }
    }

    @NonNull
    public ClientComponentsRegisterImpl getClientComponentsRegister(SplitClientConfig config,
                                                                    SplitTaskExecutor taskExecutor,
                                                                    EventsManagerCoordinator eventsManagerCoordinator,
                                                                    Synchronizer synchronizer,
                                                                    NotificationParser notificationParser,
                                                                    NotificationProcessor notificationProcessor,
                                                                    SseAuthenticator sseAuthenticator,
                                                                    SplitStorageContainer storageContainer,
                                                                    SyncManager syncManager,
                                                                    CompressionUtilProvider compressionProvider) {
        MySegmentsV2PayloadDecoder mySegmentsV2PayloadDecoder = new MySegmentsV2PayloadDecoder();

        PersistentAttributesStorage attributesStorage = null;
        if (config.persistentAttributesEnabled()) {
            attributesStorage = storageContainer.getPersistentAttributesStorage();
        }
        MySegmentsSynchronizerFactory mySegmentsSynchronizerFactory = new MySegmentsSynchronizerFactoryImpl(
                new RetryBackoffCounterTimerFactory(),
                taskExecutor,
                config.segmentsRefreshRate());

        MySegmentsNotificationProcessorFactory mySegmentsNotificationProcessorFactory = null;
        if (config.syncEnabled()) {
            mySegmentsNotificationProcessorFactory = new MySegmentsNotificationProcessorFactoryImpl(notificationParser,
                    taskExecutor,
                    mySegmentsV2PayloadDecoder,
                    compressionProvider);
        }

        return new ClientComponentsRegisterImpl(
                config,
                mySegmentsSynchronizerFactory,
                storageContainer,
                new AttributesSynchronizerFactoryImpl(taskExecutor, attributesStorage),
                (AttributesSynchronizerRegistry) synchronizer,
                (MySegmentsSynchronizerRegistry) synchronizer,
                (MySegmentsUpdateWorkerRegistry) syncManager,
                eventsManagerCoordinator,
                sseAuthenticator,
                notificationProcessor,
                mySegmentsNotificationProcessorFactory,
                mySegmentsV2PayloadDecoder);
    }

    public StreamingComponents buildStreamingComponents(@NonNull SplitTaskExecutor splitTaskExecutor,
                                                        @NonNull SplitTaskFactory splitTaskFactory,
                                                        @NonNull SplitClientConfig config,
                                                        @NonNull HttpClient defaultHttpClient,
                                                        @NonNull SplitApiFacade splitApiFacade,
                                                        @NonNull SplitStorageContainer storageContainer) {

        // Avoid creating unnecessary components if single sync enabled
        if (!config.syncEnabled()) {
            return new StreamingComponents();
        }

        BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue = new LinkedBlockingDeque<>();
        NotificationParser notificationParser = new NotificationParser();

        NotificationProcessor notificationProcessor = new NotificationProcessor(splitTaskExecutor, splitTaskFactory,
                notificationParser, splitsUpdateNotificationQueue, new MySegmentsPayloadDecoder());

        PushManagerEventBroadcaster pushManagerEventBroadcaster = new PushManagerEventBroadcaster();

        SseClient sseClient = getSseClient(config.streamingServiceUrl(),
                notificationParser,
                notificationProcessor,
                storageContainer.getTelemetryStorage(),
                pushManagerEventBroadcaster,
                defaultHttpClient);

        SseAuthenticator sseAuthenticator = new SseAuthenticator(splitApiFacade.getSseAuthenticationFetcher(),
                new SseJwtParser());

        PushNotificationManager pushNotificationManager = getPushNotificationManager(splitTaskExecutor,
                sseAuthenticator,
                pushManagerEventBroadcaster,
                sseClient,
                storageContainer.getTelemetryStorage(),
                config.defaultSSEConnectionDelay(),
                config.sseDisconnectionDelay());

        SyncGuardian syncGuardian = new SyncGuardianImpl(config);

        return new StreamingComponents(pushNotificationManager,
                splitsUpdateNotificationQueue,
                notificationParser,
                notificationProcessor,
                sseAuthenticator,
                pushManagerEventBroadcaster,
                syncGuardian);
    }

    public ProcessStrategy getImpressionStrategy(SplitTaskExecutor splitTaskExecutor,
                                                 SplitTaskFactory splitTaskFactory,
                                                 SplitStorageContainer splitStorageContainer,
                                                 SplitClientConfig config) {
        return new ImpressionStrategyProvider(splitTaskExecutor,
                splitStorageContainer,
                splitTaskFactory,
                splitStorageContainer.getTelemetryStorage(),
                config.impressionsQueueSize(),
                config.impressionsChunkSize(),
                config.impressionsRefreshRate(),
                config.impressionsCounterRefreshRate(),
                config.mtkRefreshRate(),
                config.userConsent() == UserConsent.GRANTED).getStrategy(config.impressionsMode());
    }

    SplitCipher migrateEncryption(String apiKey,
                                  SplitRoomDatabase splitDatabase,
                                  SplitTaskExecutor splitTaskExecutor,
                                  final boolean encryptionEnabled,
                                  SplitTaskExecutionListener executionListener) {

        SplitCipher toCipher = SplitCipherFactory.create(apiKey, encryptionEnabled ? SplitEncryptionLevel.AES_128_CBC :
                SplitEncryptionLevel.NONE);
        splitTaskExecutor.submit(new EncryptionMigrationTask(apiKey,
                        splitDatabase,
                        encryptionEnabled,
                        toCipher),
                executionListener);

        return toCipher;
    }

    @Nullable
    SplitUpdatesWorker getSplitUpdatesWorker(SplitClientConfig config,
                                             SplitTaskExecutor splitTaskExecutor,
                                             SplitTaskFactory splitTaskFactory,
                                             Synchronizer mSynchronizer,
                                             BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue,
                                             SplitsStorage splitsStorage,
                                             CompressionUtilProvider compressionProvider) {
        if (config.syncEnabled()) {
            return new SplitUpdatesWorker(mSynchronizer,
                    splitsUpdateNotificationQueue,
                    splitsStorage,
                    compressionProvider,
                    splitTaskExecutor,
                    splitTaskFactory);
        }

        return null;
    }

    Pair<Map<SplitFilter.Type, SplitFilter>, String> getFilterConfiguration(SyncConfig syncConfig) {
        String splitsFilterQueryString = null;
        Map<SplitFilter.Type, SplitFilter> groupedFilters = new HashMap<>();

        if (syncConfig != null) {
            FilterBuilder filterBuilder = new FilterBuilder(syncConfig.getFilters());
            groupedFilters = filterBuilder.getGroupedFilter();
            splitsFilterQueryString = filterBuilder.buildQueryString();
        }

        return new Pair<>(groupedFilters, splitsFilterQueryString);
    }

    @Nullable
    FlagSetsFilter getFlagSetsFilter(Map<SplitFilter.Type, SplitFilter> filters) {
        if (filters.get(SplitFilter.Type.BY_SET) != null) {
            return new FlagSetsFilterImpl(filters.get(SplitFilter.Type.BY_SET).getValues());
        }

        return null;
    }

    ExecutorService getImpressionsLoggingTaskExecutor() {
        return new ThreadPoolExecutor(1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private TelemetryStorage getTelemetryStorage(boolean shouldRecordTelemetry, TelemetryStorage telemetryStorage) {
        if (telemetryStorage != null) {
            return telemetryStorage;
        }
        return StorageFactory.getTelemetryStorage(shouldRecordTelemetry);
    }
}
