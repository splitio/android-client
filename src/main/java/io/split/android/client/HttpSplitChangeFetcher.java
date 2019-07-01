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
import io.split.android.client.cache.SplitChangeCache;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpResponse;
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

    private final HttpClient _client;
    private final URI _target;
    private final Metrics _metrics;
    private final ISplitChangeCache _splitChangeCache;

    public static HttpSplitChangeFetcher create(HttpClient client, URI root, ISplitChangeCache splitChangeCache) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics(), splitChangeCache);
    }

    public static HttpSplitChangeFetcher create(HttpClient client, URI root, Metrics metrics, ISplitChangeCache splitChangeCache) throws URISyntaxException {
        return new HttpSplitChangeFetcher(client, new URIBuilder(root).setPath("/api/splitChanges").build(), metrics, splitChangeCache);
    }

    private HttpSplitChangeFetcher(HttpClient client, URI uri, Metrics metrics, ISplitChangeCache splitChangeCache) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        _splitChangeCache = splitChangeCache;

        checkNotNull(_target);
    }

    @Override
    public SplitChange fetch(long since) {
        return fetch(since, FetcherPolicy.NetworkAndCache);
    }

    @Override
    public SplitChange fetch(long since, FetcherPolicy fetcherPolicy) {

        long start = System.currentTimeMillis();

        if (fetcherPolicy == FetcherPolicy.CacheOnly) {
            Logger.d("First load... USING PERSISTED");
            return _splitChangeCache.getChanges(since);
        }

        if (!isSourceReachable()) {
            throw new IllegalStateException("Problem fetching splitChanges: Source not reachable");
        }



        try {
            URI uri = new URIBuilder(_target).addParameter(SINCE, "" + since).build();

            HttpResponse response = _client.request(uri, HttpClient.HTTP_GET).execute();

            //TODO: Reason
            String reason = "";
            if (!response.isSuccess()) {
                _metrics.count(PREFIX + ".status." + response.getHttpStatus(), 1);
                throw new IllegalStateException("Could not retrieve splitChanges; http return code " + response.getHttpStatus());
            }

            SplitChange splitChange = Json.fromJson(response.getData(), SplitChange.class);

            _splitChangeCache.addChange(splitChange);

            return splitChange;
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching splitChanges: " + t.getMessage(), t);
        } finally {
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean isSourceReachable() {
        return Utils.isReachable(_target);
    }

}
