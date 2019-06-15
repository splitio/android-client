package io.split.android.client;

import com.google.gson.reflect.TypeToken;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.dtos.MySegment;
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

    private final CloseableHttpClient _client;
    private final URI _target;
    private final Metrics _metrics;
    private final IMySegmentsCache _mySegmentsCache;

    public HttpMySegmentsFetcher(CloseableHttpClient client, URI uri, Metrics metrics, IStorage storage) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        _mySegmentsCache = new MySegmentsCache(storage);
        checkNotNull(_target);
    }

    public static HttpMySegmentsFetcher create(CloseableHttpClient client, URI root, IStorage storage) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics(), storage);
    }

    public static HttpMySegmentsFetcher create(CloseableHttpClient client, URI root, Metrics metrics, IStorage storage) throws URISyntaxException {
        return new HttpMySegmentsFetcher(client, new URIBuilder(root).setPath("/api/mySegments").build(), metrics, storage);
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

        CloseableHttpResponse response = null;

        try {
            String path = String.format("%s/%s", _target.getPath(), matchingKey);
            URI uri = new URIBuilder(_target).setPath(path).build();
            HttpGet request = new HttpGet(uri);
            response = _client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                Logger.e(String.format("Response status was: %d", statusCode));
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve mySegments for " + matchingKey + "; http return code " + statusCode);
            }

            String json = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);

            Logger.d("Received json: %s", json);
            Type mapType = new TypeToken<Map<String, List<MySegment>>>() {
            }.getType();

            Map<String, List<MySegment>> mySegmentsMap = Json.fromJson(json, mapType);

            List<MySegment> mySegmentList = mySegmentsMap.get("mySegments");

            _mySegmentsCache.setMySegments(matchingKey, mySegmentList);

            return mySegmentList;
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }
    }
}
