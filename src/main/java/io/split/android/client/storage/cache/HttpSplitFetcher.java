package io.split.android.client.storage.cache;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public final class HttpSplitFetcher implements NewSplitFetcher {

    private static final String SINCE_PARAMETER = "since";

    private final HttpClient _client;
    private final URI _target;
    private final Metrics _metrics;

    public static HttpSplitFetcher create(HttpClient client, URI root) throws URISyntaxException {
        return create(client, root, new Metrics.NoopMetrics());
    }

    public static HttpSplitFetcher create(HttpClient client, URI root, Metrics metrics) throws URISyntaxException {
        return new HttpSplitFetcher(client, new URIBuilder(root, SdkTargetPath.SPLIT_CHANGES).build(), metrics);
    }

    private HttpSplitFetcher(@NonNull HttpClient client, @NonNull URI target, Metrics metrics) {
        _client = client;
        _target = target;
        _metrics = metrics;

        checkNotNull(_target);
    }

    @Override
    public SplitChange execute(long since) {

        long start = System.currentTimeMillis();

        if (!isSourceReachable()) {
            throw new IllegalStateException("Problem fetching splitChanges: Source not reachable");
        }

        try {
            URI uri = new URIBuilder(_target).addParameter(SINCE_PARAMETER, "" + since).build();

            HttpResponse response = _client.request(uri, HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                _metrics.count(Metrics.SPLIT_CHANGES_FETCHER_STATUS_OK, 1);
                throw new IllegalStateException("Could not retrieve splitChanges; http return code " + response.getHttpStatus());
            }

            SplitChange splitChange = Json.fromJson(response.getData(), SplitChange.class);

            return splitChange;
        } catch (Throwable t) {
            _metrics.count(Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION, 1);
            throw new IllegalStateException("Problem fetching splitChanges: " + t.getMessage(), t);
        } finally {
            _metrics.time(Metrics.SPLIT_CHANGES_FETCHER_TIME, System.currentTimeMillis() - start);
        }
    }

    private boolean isSourceReachable() {
        return Utils.isReachable(_target);
    }

}
