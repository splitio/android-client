package io.split.android.client.metrics;

import com.google.common.collect.Lists;

import io.split.android.client.dtos.Counter;
import io.split.android.client.dtos.Latency;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.engine.metrics.Metrics;



import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpMetrics implements Metrics, DTOMetrics {

    private final HttpClient _client;
    private final URI _target;


    public static HttpMetrics create(HttpClient client, URI root) throws URISyntaxException {
        return new HttpMetrics(client, root);
    }


    public HttpMetrics(HttpClient client, URI uri) {
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
            post(new URIBuilder(_target, "/api/metrics/time").build(), dto);
        } catch (Throwable t) {
            Logger.e(t, "Exception when posting metric %s", dto);
        }

    }

    @Override
    public void count(Counter dto) {

        try {
            post(new URIBuilder(_target, "/api/metrics/counter").build(), dto);
        } catch (Throwable t) {
            Logger.e(t, "Exception when posting metric %s", dto);
        }
    }

    private void post(URI uri, Object dto) {

        try {
            String jsonMetrics = Json.toJson(dto);
            HttpResponse response = _client.request(uri, HttpClient.HTTP_POST, jsonMetrics).execute();

            if (!response.isSuccess()) {
                Logger.w("Response status was: %d", response.getHttpStatus());
            }
        } catch (Throwable t) {
            Logger.e(t,"Exception when posting metrics");
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
            Logger.e(t, "Could not count metric %s", counter);
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
            Logger.e(t, "Could not time metric %s", operation);
        }
    }

}
