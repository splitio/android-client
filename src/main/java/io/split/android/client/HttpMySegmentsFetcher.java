package io.split.android.client;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;
import io.split.android.engine.experiments.FetcherPolicy;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.segments.MySegmentsFetcher;

import static com.google.common.base.Preconditions.checkNotNull;


public final class HttpMySegmentsFetcher implements MySegmentsFetcher {

    private static final String PREFIX = "mySegmentsFetcher";

    private final HttpClient _client;
    private final URI _target;
    private final Metrics _metrics;
    private final IMySegmentsCache _mySegmentsCache;
    static final private Type _mySegmentsJsonMapType = new TypeToken<Map<String, List<MySegment>>>() {}.getType();

    public HttpMySegmentsFetcher(HttpClient client, URI uri, Metrics metrics, IMySegmentsCache mySegmentsCache) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        _mySegmentsCache = mySegmentsCache;
        checkNotNull(_target);
    }

    public static HttpMySegmentsFetcher create(HttpClient client, URI root, IMySegmentsCache mySegmentsCache) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics(), mySegmentsCache);
    }

    public static HttpMySegmentsFetcher create(HttpClient client, URI root, Metrics metrics, IMySegmentsCache mySegmentsCache) throws URISyntaxException {
        return new HttpMySegmentsFetcher(client, new URIBuilder(root, "/mySegments").build(), metrics, mySegmentsCache);
    }

    @Override
    public List<MySegment> fetch(String matchingKey) {
        return fetch(matchingKey, FetcherPolicy.NetworkAndCache);
    }

    @Override
    public List<MySegment> fetch(String matchingKey, FetcherPolicy fetcherPolicy) {
        long start = System.currentTimeMillis();

        if (fetcherPolicy == FetcherPolicy.NetworkAndCache && !Utils.isReachable(_target)) {
            Logger.d(String.format("%s is NOT REACHABLE... USING PERSISTED", _target.getHost()));
            return _mySegmentsCache.getMySegments(matchingKey);
        } else if (fetcherPolicy == FetcherPolicy.CacheOnly) {
            Logger.d("First load... USING PERSISTED");
            return _mySegmentsCache.getMySegments(matchingKey);
        }

        try {
            URI uri = new URIBuilder(_target, matchingKey).build();
            HttpResponse response = _client.request(uri, HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                int statusCode = response.getHttpStatus();
                Logger.e(String.format("Response status was: %d", statusCode));
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve mySegments for " + matchingKey + "; http return code " + statusCode);
            };

            Logger.d("Received json: %s", response.getData());
            Map<String, List<MySegment>> mySegmentsMap = Json.fromJson(response.getData(), _mySegmentsJsonMapType);
            List<MySegment> mySegmentList = mySegmentsMap.get("mySegments");
            _mySegmentsCache.setMySegments(matchingKey, mySegmentList);

            return mySegmentList;
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }
    }
}
