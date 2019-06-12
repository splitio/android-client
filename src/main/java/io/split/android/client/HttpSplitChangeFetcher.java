package io.split.android.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.cache.ISplitChangeCache;
import io.split.android.client.cache.ITrafficTypesCache;
import io.split.android.client.cache.SplitChangeCache;
import io.split.android.client.cache.TrafficTypesCache;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;
import io.split.android.engine.experiments.FetcherPolicy;
import io.split.android.engine.experiments.SplitChangeFetcher;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public final class HttpSplitChangeFetcher implements SplitChangeFetcher {

    private static final String SINCE = "since";
    private static final String PREFIX = "splitChangeFetcher";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final Metrics _metrics;
    private final ISplitChangeCache _splitChangeCache;
    private final ITrafficTypesCache _trafficTypesCache;

    public static HttpSplitChangeFetcher create(CloseableHttpClient client, URI root, IStorage storage, ITrafficTypesCache trafficTypesCache) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics(), storage, trafficTypesCache);
    }

    public static HttpSplitChangeFetcher create(CloseableHttpClient client, URI root, Metrics metrics, IStorage storage, ITrafficTypesCache trafficTypesCache) throws URISyntaxException {
        return new HttpSplitChangeFetcher(client, new URIBuilder(root).setPath("/api/splitChanges").build(), metrics, storage, trafficTypesCache);
    }

    private HttpSplitChangeFetcher(CloseableHttpClient client, URI uri, Metrics metrics, IStorage storage, ITrafficTypesCache trafficTypesCache) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        _splitChangeCache = new SplitChangeCache(storage);
        _trafficTypesCache = trafficTypesCache;
        _trafficTypesCache.setFromSplits(_splitChangeCache.getChanges(-1).splits);
        checkNotNull(_target);
    }

    @Override
    public SplitChange fetch(long since) {
        return fetch(since, FetcherPolicy.NetworkAndCache);
    }

    @Override
    public SplitChange fetch(long since, FetcherPolicy fetcherPolicy) {

        long start = System.currentTimeMillis();

        if (fetcherPolicy == FetcherPolicy.NetworkAndCache && !Utils.isReachable(_target)) {
            Logger.d("%s is NOT REACHABLE... USING PERSISTED", _target.getHost());
            return _splitChangeCache.getChanges(since);
        } else if (fetcherPolicy == FetcherPolicy.CacheOnly) {
            Logger.d("First load... USING PERSISTED");
            return _splitChangeCache.getChanges(since);
        }

        CloseableHttpResponse response = null;

        try {
            URI uri = new URIBuilder(_target).addParameter(SINCE, "" + since).build();

            HttpGet request = new HttpGet(uri);
            response = _client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve splitChanges; http return code " + statusCode);
            }


            String json = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            Logger.d("Received json: %s", json);

            SplitChange splitChange = Json.fromJson(json, SplitChange.class);

            _splitChangeCache.addChange(splitChange);
            _trafficTypesCache.updateFromSplits(splitChange.splits);

            return splitChange;
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching splitChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }
    }

}
