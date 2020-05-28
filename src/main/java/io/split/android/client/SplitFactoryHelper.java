package io.split.android.client;

import android.content.Context;

import androidx.work.WorkManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.api.Key;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.SseClient;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.Metrics;

class SplitFactoryHelper {

    String buildDatabaseName(SplitClientConfig splitClientConfig, String apiToken) {
        String databaseName = Utils.convertApiKeyToFolder(apiToken);
        if (databaseName == null) {
            databaseName = splitClientConfig.defaultDataFolder();
        }
        return databaseName;
    }

    Map<String, String> buildHeaders(SplitClientConfig splitClientConfig, String apiToken) {
        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setHostIp(splitClientConfig.ip());
        headersBuilder.setHostname(splitClientConfig.hostname());
        headersBuilder.setClientVersion(SplitClientConfig.splitSdkVersion);
        headersBuilder.setApiToken(apiToken);
        return headersBuilder.build();
    }

    SplitStorageContainer buildStorageContainer(Context context, Key key, String databaseName) {
        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        return new SplitStorageContainer(
                StorageFactory.getSplitsStorage(splitRoomDatabase),
                StorageFactory.getMySegmentsStorage(splitRoomDatabase, key.matchingKey()),
                StorageFactory.getPersistenEventsStorage(splitRoomDatabase),
                StorageFactory.getPersistenImpressionsStorage(splitRoomDatabase));
    }

    SplitApiFacade buildApiFacade(SplitClientConfig splitClientConfig,
                                  Key key,
                                  HttpClient httpClient,
                                  Metrics cachedFireAndForgetMetrics) throws URISyntaxException {
        NetworkHelper networkHelper = new NetworkHelper();

        return new SplitApiFacade(
                ServiceFactory.getSplitsFetcher(networkHelper, httpClient,
                        splitClientConfig.endpoint(), cachedFireAndForgetMetrics),
                ServiceFactory.getMySegmentsFetcher(networkHelper, httpClient,
                        splitClientConfig.endpoint(), key.matchingKey(), cachedFireAndForgetMetrics),
                ServiceFactory.getSseAuthenticationFetcher(networkHelper, httpClient,
                        splitClientConfig.authServiceUrl()),
                ServiceFactory.getEventsRecorder(networkHelper, httpClient,
                        splitClientConfig.eventsEndpoint()),
                ServiceFactory.getImpressionsRecorder(networkHelper, httpClient,
                        splitClientConfig.eventsEndpoint()));
    }

    WorkManagerWrapper buildWorkManagerWrapper(Context context, SplitClientConfig splitClientConfig,
                                               String apiKey, String key, String databaseName) {
        return new WorkManagerWrapper(
                WorkManager.getInstance(context), splitClientConfig, apiKey, key, databaseName);

    }

    SyncManager buildSyncManager(SplitClientConfig config,
                                 SplitTaskExecutor splitTaskExecutor,
                                 SplitTaskFactory splitTaskFactory,
                                 HttpClient httpClient,
                                 Synchronizer synchronizer) {

        BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue
                = new LinkedBlockingDeque<>();

        BlockingQueue<MySegmentChangeNotification> mySegmentChangeNotificationQueue
                = new LinkedBlockingDeque<>();

        SplitUpdatesWorker splitUpdateWorker = new SplitUpdatesWorker(synchronizer,
                splitsUpdateNotificationQueue);
        MySegmentsUpdateWorker mySegmentUpdateWorker = new MySegmentsUpdateWorker(synchronizer,
                mySegmentChangeNotificationQueue);

        NotificationParser notificationParser = new NotificationParser();
        NotificationProcessor notificationProcessor =
                new NotificationProcessor(splitTaskExecutor, splitTaskFactory,
                        notificationParser, mySegmentChangeNotificationQueue,
                        splitsUpdateNotificationQueue);
        PushManagerEventBroadcaster pushManagerEventBroadcaster = new PushManagerEventBroadcaster();

        URI streamingServiceUrl = URI.create(config.streamingServiceUrl());
        EventStreamParser eventStreamParser = new EventStreamParser();
        SseClient sseClient =
                new SseClient(streamingServiceUrl, new HttpClientImpl(config.getStreamingConnectionTimeout()), eventStreamParser);
        PushNotificationManager pushNotificationManager =
                new PushNotificationManager(sseClient, splitTaskExecutor,
                        splitTaskFactory, notificationParser,
                        notificationProcessor, pushManagerEventBroadcaster,
                        new ReconnectBackoffCounter(config.authRetryBackoffBase()),
                        new ReconnectBackoffCounter(config.streamingReconnectBackoffBase()));

        return new SyncManagerImpl(
                config, synchronizer, pushNotificationManager, splitUpdateWorker,
                mySegmentUpdateWorker, pushManagerEventBroadcaster);
    }
}
