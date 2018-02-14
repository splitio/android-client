package io.split.android.client.metrics;

import com.google.common.collect.Lists;

import io.split.android.client.dtos.Counter;
import io.split.android.client.dtos.Latency;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.Metrics;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import timber.log.Timber;


import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpMetrics implements Metrics, DTOMetrics {

    private final CloseableHttpClient _client;
    private final URI _target;


    public static HttpMetrics create(CloseableHttpClient client, URI root) throws URISyntaxException {
        return new HttpMetrics(client, new URIBuilder(root).build());
    }


    public HttpMetrics(CloseableHttpClient client, URI uri) {
        _client = client;
        _target = uri;
        checkNotNull(_client);
        checkNotNull(_target);
    }


    @Override
    public void time(Latency dto) {

        if (dto.latencies.isEmpty()) {
            return;
        }

        try {
            post(new URIBuilder(_target).setPath("/api/metrics/time").build(), dto);
        } catch (Throwable t) {
            Timber.w(t, "Exception when posting metric %s", dto);
        }

    }

    @Override
    public void count(Counter dto) {

        try {
            post(new URIBuilder(_target).setPath("/api/metrics/counter").build(), dto);
        } catch (Throwable t) {
            Timber.w(t, "Exception when posting metric %s", dto);
        }

    }

    private void post(URI uri, Object dto) {

        CloseableHttpResponse response = null;

        try {
            StringEntity entity = Utils.toJsonEntity(dto);

            HttpPost request = new HttpPost(uri);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                Timber.w("Response status was: %i", status);
            }

        } catch (Throwable t) {
            Timber.w("Exception when posting metrics: %s", t.getMessage());
            Timber.d(t);
        } finally {
            Utils.forceClose(response);
        }

    }

    @Override
    public void count(String counter, long delta) {
        try {
            Counter dto = new Counter();
            dto.name = counter;
            dto.delta = delta;

            count(dto);
        } catch (Throwable t) {
            Timber.i(t, "Could not count metric %s", counter);
        }

    }

    @Override
    public void time(String operation, long timeInMs) {
        try {
            Latency dto = new Latency();
            dto.name = operation;
            dto.latencies = Lists.newArrayList(timeInMs);

            time(dto);
        } catch (Throwable t) {
            Timber.i(t, "Could not time metric %s", operation);
        }
    }

}
