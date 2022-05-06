package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.api.Key;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.http.mysegments.MySegmentsFetcherFactoryImpl;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
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
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.shared.ClientComponentsRegister;
import io.split.android.client.shared.ClientComponentsRegisterImpl;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.TelemetrySynchronizerImpl;
import io.split.android.client.telemetry.TelemetrySynchronizerStub;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.NetworkHelper;
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
        int apiTokenLength = apiToken.length();
        if (apiTokenLength > DB_MAGIC_CHARS_COUNT) {
            String begin = apiToken.substring(0, DB_MAGIC_CHARS_COUNT);
            String end = apiToken.substring(apiTokenLength - DB_MAGIC_CHARS_COUNT);
            return begin + end;
        }
        return config.defaultDataFolder();
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

    SplitStorageContainer buildStorageContainer(SplitRoomDatabase splitRoomDatabase, Key key, boolean shouldRecordTelemetry) {
        return new SplitStorageContainer(
                StorageFactory.getSplitsStorage(splitRoomDatabase),
                StorageFactory.getMySegmentsStorage(splitRoomDatabase),
                StorageFactory.getPersistentSplitsStorage(splitRoomDatabase),
                StorageFactory.getPersistenEventsStorage(splitRoomDatabase),
                StorageFactory.getPersistenImpressionsStorage(splitRoomDatabase),
                StorageFactory.getPersistenImpressionsCountStorage(splitRoomDatabase),
                StorageFactory.getAttributesStorage(),
                StorageFactory.getPersistentSplitsStorage(splitRoomDatabase, key.matchingKey()),
                StorageFactory.getTelemetryStorage(shouldRecordTelemetry));
    }

    String buildSplitsFilterQueryString(SplitClientConfig config) {
        SyncConfig syncConfig = config.syncConfig();
        if (syncConfig != null) {
            return new FilterBuilder().addFilters(syncConfig.getFilters()).build();
        }
        return null;
    }

    SplitApiFacade buildApiFacade(SplitClientConfig splitClientConfig,
                                  HttpClient httpClient,
                                  String splitsFilterQueryString) throws URISyntaxException {
        NetworkHelper networkHelper = new NetworkHelper();

        return new SplitApiFacade(
                ServiceFactory.getSplitsFetcher(networkHelper, httpClient,
                        splitClientConfig.endpoint(), splitsFilterQueryString),
                new MySegmentsFetcherFactoryImpl(networkHelper, httpClient,
                        splitClientConfig.endpoint()),
                ServiceFactory.getSseAuthenticationFetcher(networkHelper, httpClient,
                        splitClientConfig.authServiceUrl()),
                ServiceFactory.getEventsRecorder(networkHelper, httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getImpressionsRecorder(networkHelper, httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getImpressionsCountRecorder(networkHelper, httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getUniqueKeysRecorder(networkHelper, httpClient,
                        splitClientConfig.telemetryEndpoint()),
                ServiceFactory.getTelemetryConfigRecorder(networkHelper, httpClient,
                        splitClientConfig.telemetryEndpoint()),
                ServiceFactory.getTelemetryStatsRecorder(networkHelper, httpClient,
                        splitClientConfig.telemetryEndpoint()));
    }

    WorkManagerWrapper buildWorkManagerWrapper(Context context, SplitClientConfig splitClientConfig,
                                               String apiKey, String databaseName) {
        return new WorkManagerWrapper(
                WorkManager.getInstance(context), splitClientConfig, apiKey, databaseName);

    }

    SyncManager buildSyncManager(SplitClientConfig config,
                                 SplitTaskExecutor splitTaskExecutor,
                                 Synchronizer synchronizer,
                                 TelemetrySynchronizer telemetrySynchronizer,
                                 PushNotificationManager pushNotificationManager,
                                 BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue,
                                 PushManagerEventBroadcaster pushManagerEventBroadcaster) {

        return new SyncManagerImpl(config,
                synchronizer,
                pushNotificationManager,
                new SplitUpdatesWorker(synchronizer, splitsUpdateNotificationQueue),
                pushManagerEventBroadcaster,
                new BackoffCounterTimer(splitTaskExecutor, new ReconnectBackoffCounter(1)),
                telemetrySynchronizer);
    }

    @NonNull
    PushNotificationManager getPushNotificationManager(SplitTaskExecutor _splitTaskExecutor,
                                                       SseAuthenticator sseAuthenticator,
                                                       PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                                       SseClient sseClient,
                                                       TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new PushNotificationManager(pushManagerEventBroadcaster,
                sseAuthenticator,
                sseClient,
                new SseRefreshTokenTimer(_splitTaskExecutor, pushManagerEventBroadcaster),
                telemetryRuntimeProducer,
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
                                                                    String defaultMatchingKey) {
        MySegmentsV2PayloadDecoder mySegmentsV2PayloadDecoder = new MySegmentsV2PayloadDecoder();

        return new ClientComponentsRegisterImpl(
                new MySegmentsSynchronizerFactoryImpl(new RetryBackoffCounterTimerFactory(),
                        taskExecutor,
                        config.segmentsRefreshRate()),
                storageContainer,
                new AttributesSynchronizerFactoryImpl(taskExecutor, config.persistentAttributesEnabled() ? storageContainer.getPersistentAttributesStorage() : null),
                (AttributesSynchronizerRegistry) synchronizer,
                (MySegmentsSynchronizerRegistry) synchronizer,
                (MySegmentsUpdateWorkerRegistry) syncManager,
                eventsManagerCoordinator,
                sseAuthenticator,
                notificationProcessor,
                defaultMatchingKey,
                new MySegmentsNotificationProcessorFactoryImpl(notificationParser,
                        taskExecutor,
                        mySegmentsV2PayloadDecoder,
                        new CompressionUtilProvider()),
                mySegmentsV2PayloadDecoder);
    }
}
