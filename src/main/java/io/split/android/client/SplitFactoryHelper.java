package io.split.android.client;

import android.content.Context;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.events.EventsRequestBodySerializer;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderImpl;
import io.split.android.client.service.impressions.ImpressionsRequestBodySerializer;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.FetcherMetricsConfig;
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
        PersistentMySegmentsStorage persistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(splitRoomDatabase, key.matchingKey());
        MySegmentsStorage mySegmentsStorage = new MySegmentsStorageImpl(persistentMySegmentsStorage);
        PersistentImpressionsStorage persistentImpressionsStorage = new SqLitePersistentImpressionsStorage(splitRoomDatabase, 100);
        PersistentEventsStorage persistentEventsStorage = new SqLitePersistentEventsStorage(splitRoomDatabase, 100);
        PersistentSplitsStorage persistentSplitsStorage = new SqLitePersistentSplitsStorage(splitRoomDatabase);
        SplitsStorage splitsStorage = new SplitsStorageImpl(persistentSplitsStorage);
        return new SplitStorageContainer(
                splitsStorage, mySegmentsStorage,
                persistentEventsStorage, persistentImpressionsStorage);
    }

    SplitApiFacade buildApiFacade(SplitClientConfig splitClientConfig,
                                          Key key,
                                          HttpClient httpClient,
                                          Metrics cachedFireAndForgetMetrics) throws URISyntaxException {
        NetworkHelper networkHelper = new NetworkHelper();

        FetcherMetricsConfig splitsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION,
                Metrics.SPLIT_CHANGES_FETCHER_TIME,
                Metrics.SPLIT_CHANGES_FETCHER_STATUS
        );

        HttpFetcher<SplitChange> splitsFetcher = new HttpFetcherImpl<SplitChange>(httpClient,
                SdkTargetPath.splitChanges(splitClientConfig.endpoint()), cachedFireAndForgetMetrics,
                splitsfetcherMetricsConfig,
                networkHelper, new SplitChangeResponseParser());

        FetcherMetricsConfig mySegmentsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.MY_SEGMENTS_FETCHER_EXCEPTION,
                Metrics.MY_SEGMENTS_FETCHER_TIME,
                Metrics.MY_SEGMENTS_FETCHER_STATUS
        );

        HttpFetcher<List<MySegment>> mySegmentsFetcher = new HttpFetcherImpl<>(httpClient,
                SdkTargetPath.mySegments(splitClientConfig.endpoint(), key.matchingKey()), cachedFireAndForgetMetrics,
                mySegmentsfetcherMetricsConfig,
                networkHelper, new MySegmentsResponseParser());

        HttpRecorder<List<Event>> eventsRecorder = new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.events(splitClientConfig.eventsEndpoint()), networkHelper,
                new EventsRequestBodySerializer());

        HttpRecorder<List<KeyImpression>> impressionsRecorder = new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.impressions(splitClientConfig.eventsEndpoint()), networkHelper,
                new ImpressionsRequestBodySerializer());


        return new SplitApiFacade(
                splitsFetcher, mySegmentsFetcher,
                eventsRecorder, impressionsRecorder);

    }
}
