package io.split.android.client;

import com.google.gson.reflect.TypeToken;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.segments.MySegmentsFetcher;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;


public final class HttpMySegmentsFetcher implements MySegmentsFetcher {

    private static final String PREFIX = "mySegmentsFetcher";

    private final CloseableHttpClient _client;
    private final URI _target;
    private final Metrics _metrics;

    public static HttpMySegmentsFetcher create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics());
    }

    public static HttpMySegmentsFetcher create(CloseableHttpClient client, URI root, Metrics metrics) throws URISyntaxException {
        return new HttpMySegmentsFetcher(client, new URIBuilder(root).setPath("/api/mySegments").build(), metrics);
    }

    public HttpMySegmentsFetcher(CloseableHttpClient client, URI uri, Metrics metrics) {
        _client = client;
        _target = uri;
        _metrics = metrics;
        checkNotNull(_target);
    }

    @Override
    public List<MySegment> fetch(String matchingKey) {
        long start = System.currentTimeMillis();

        CloseableHttpResponse response = null;

        try {
            String path = String.format("%s/%s", _target.getPath(), matchingKey);
            URI uri = new URIBuilder(_target).setPath(path).build();
            HttpGet request = new HttpGet(uri);
            response = _client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                Timber.e("Response status was: %i", statusCode);
                _metrics.count(PREFIX + ".status." + statusCode, 1);
                throw new IllegalStateException("Could not retrieve mySegments for " + matchingKey + "; http return code " + statusCode);
            }

            String json = EntityUtils.toString(response.getEntity());

            Timber.d("Received json: %s", json);
            Type mapType = new TypeToken<Map<String,MySegment[]>>(){}.getType();
            Map<String,MySegment[]> mySegmentsMap = Json.fromJson(json, mapType);
            MySegment[] mySegmentsArray = mySegmentsMap.get("mySegments");
            return Arrays.asList(mySegmentsArray);
        } catch (Throwable t) {
            _metrics.count(PREFIX + ".exception", 1);
            throw new IllegalStateException("Problem fetching segmentChanges: " + t.getMessage(), t);
        } finally {
            Utils.forceClose(response);
            _metrics.time(PREFIX + ".time", System.currentTimeMillis() - start);
        }
    }
}
