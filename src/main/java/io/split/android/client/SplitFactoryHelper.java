package io.split.android.client;

import android.content.Context;

import androidx.work.WorkManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.api.Key;
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
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.sseclient.sseclient.SseClientImpl;
import io.split.android.client.service.sseclient.sseclient.SseHandler;
import io.split.android.client.service.sseclient.sseclient.SseRefreshTokenTimer;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.client.utils.Utils;

class SplitFactoryHelper {
    private static final int DB_MAGIC_CHARS_COUNT = 4;

    String getDatabaseName(SplitClientConfig config, String apiToken, Context context) {

        String dbName = buildDatabaseName(config, apiToken);
        File dbPath = context.getDatabasePath(dbName);
        if(dbPath.exists()) {
            return dbName;
        }

        String legacyName = buildLegacyDatabaseName(config, apiToken);
        File legacyDb = context.getDatabasePath(legacyName);
        if(legacyDb.exists()) {
            legacyDb.renameTo(dbPath);
        }
        return dbName;
    }

    private String buildDatabaseName(SplitClientConfig config, String apiToken) {
        int apiTokenLength = apiToken.length();
        if(apiTokenLength > DB_MAGIC_CHARS_COUNT) {
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
                ServiceFactory.getTelemetryConfigRecorder(networkHelper, httpClient,
                        splitClientConfig.telemetryEndpoint()),
                ServiceFactory.getTelemetryStatsRecorder(networkHelper, httpClient,
                        splitClientConfig.telemetryEndpoint()));
    }

    WorkManagerWrapper buildWorkManagerWrapper(Context context, SplitClientConfig splitClientConfig,
                                               String apiKey, Set<String> keys, String databaseName) {
        return new WorkManagerWrapper(
                WorkManager.getInstance(context), splitClientConfig, apiKey, databaseName, keys);

    }

    SyncManager buildSyncManager(SplitClientConfig config,
                                 SplitTaskExecutor splitTaskExecutor,
                                 SplitTaskFactory splitTaskFactory,
                                 HttpClient httpClient,
                                 Synchronizer synchronizer,
                                 TelemetrySynchronizer telemetrySynchronizer,
                                 TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 SseAuthenticator sseAuthenticator) {

        BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue
                = new LinkedBlockingDeque<>();

        SplitUpdatesWorker splitUpdateWorker = new SplitUpdatesWorker(synchronizer,
                splitsUpdateNotificationQueue);

        NotificationParser notificationParser = new NotificationParser();
        NotificationProcessor notificationProcessor =
                new NotificationProcessor(splitTaskExecutor, splitTaskFactory,
                        notificationParser, splitsUpdateNotificationQueue);
        PushManagerEventBroadcaster pushManagerEventBroadcaster = new PushManagerEventBroadcaster();

        URI streamingServiceUrl = URI.create(config.streamingServiceUrl());
        EventStreamParser eventStreamParser = new EventStreamParser();

        SseHandler sseHandler = new SseHandler(notificationParser,
                notificationProcessor,
                telemetryRuntimeProducer,
                pushManagerEventBroadcaster);

        SseClientImpl sseClient = new SseClientImpl(streamingServiceUrl,
                httpClient,
                eventStreamParser,
                sseHandler);

        PushNotificationManager pushNotificationManager =
                new PushNotificationManager(pushManagerEventBroadcaster,
                        sseAuthenticator,
                        sseClient,
                        new SseRefreshTokenTimer(splitTaskExecutor, pushManagerEventBroadcaster),
                        telemetryRuntimeProducer,
                        null);

        BackoffCounterTimer backoffReconnectTimer = new BackoffCounterTimer(splitTaskExecutor, new ReconnectBackoffCounter(1));

        return new SyncManagerImpl(config, synchronizer, pushNotificationManager, splitUpdateWorker, pushManagerEventBroadcaster, backoffReconnectTimer, telemetrySynchronizer);
    }
}
