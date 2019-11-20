package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.BaseHttpFetcher;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public final class HttpSplitFetcher extends BaseHttpFetcher implements SplitFetcherV2 {

    private static final String SINCE_PARAMETER = "since";

    public static HttpSplitFetcher create(HttpClient client, URI root, Metrics metrics, NetworkHelper networkHelper) throws URISyntaxException {
        return new HttpSplitFetcher(client, new URIBuilder(root, SdkTargetPath.SPLIT_CHANGES).build(), metrics, networkHelper);
    }

    public HttpSplitFetcher(@NonNull HttpClient client, @NonNull URI target, @NonNull Metrics metrics, @NonNull NetworkHelper networkHelper) {
        super(client, target, metrics, networkHelper);
    }

    @Override
    public SplitChange execute(long since) {

        long start = System.currentTimeMillis();
        SplitChange splitChange = null;

        try {
            checkIfSourceIsReachable();
            URI uri = new URIBuilder(mTarget).addParameter(SINCE_PARAMETER, Long.toString(since)).build();
            HttpResponse response = executeRequest(uri);

            checkResponseStatusAndThrowOnFail(response, Metrics.SPLIT_CHANGES_FETCHER_STATUS);

            splitChange = Json.fromJson(response.getData(), SplitChange.class);

            if (splitChange == null) {
                throwIlegalStatusException("Wrong data received from split changes server");
            }
        } catch (Exception e) {
            metricExceptionAndThrow(Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION, e);
        } finally {
            metricTime(Metrics.SPLIT_CHANGES_FETCHER_TIME, System.currentTimeMillis() - start);
        }
        return splitChange;
    }
}
