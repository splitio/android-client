package io.split.android.client;

import io.split.android.client.dtos.SegmentChange;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.segments.SegmentChangeFetcher;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import timber.log.Timber;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/22/15.
 */
public final class HttpSegmentChangeFetcher implements SegmentChangeFetcher {

    private static final String SINCE = "since";
    private static final String PREFIX = "segmentChangeFetcher";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final Metrics _metrics;

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics());
    }

    public static HttpSegmentChangeFetcher create(CloseableHttpClient client, URI root, Metrics metrics) throws URISyntaxException {
        return new HttpSegmentChangeFetcher(client, new URIBuilder(root).setPath("/api/segmentChanges").build(), metrics);
    }

    private HttpSegmentChangeFetcher(CloseableHttpClient client, URI uri, Metrics metrics) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        checkNotNull(_target);
    }

    @Override
    public SegmentChange fetch(String segmentName, long since) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            String path = _target.getPath() + "/" + segmentName;
            URI uri = new URIBuilder(_target).setPath(path).addParameter(SINCE, "" + since).build();
            HttpGet request = new HttpGet(uri);
            response = _client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                Timber.e("Response status was: %i", statusCode);
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve segment changes for " + segmentName + "; http return code " + statusCode);
            }


            String json = EntityUtils.toString(response.getEntity());

            Timber.d("Received json: %s", json);
            return Json.fromJson(json, SegmentChange.class);
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }


    }
}
