package io.split.android.client.service.splits;

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
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public final class HttpSplitFetcher implements SplitFetcherV2 {

    private static final String SINCE_PARAMETER = "since";

    private final HttpClient mClient;
    private final URI mTarget;
    private final Metrics mMetrics;
    private final NetworkHelper mNetworkHelper;

    public static HttpSplitFetcher create(HttpClient client, URI root, Metrics metrics, NetworkHelper networkHelper) throws URISyntaxException {
        return new HttpSplitFetcher(client, new URIBuilder(root, SdkTargetPath.SPLIT_CHANGES).build(), metrics, networkHelper);
    }

    private HttpSplitFetcher(@NonNull HttpClient client, @NonNull URI target, Metrics metrics, NetworkHelper networkHelper) {
        mClient = client;
        mTarget = target;
        mMetrics = metrics;
        mNetworkHelper = networkHelper;

        checkNotNull(mTarget);
    }

    @Override
    public SplitChange execute(long since) {

        long start = System.currentTimeMillis();

        if (!isSourceReachable()) {
            throw new IllegalStateException(exceptionMessage("Source not reachable"));
        }

        try {
            URI uri = new URIBuilder(mTarget).addParameter(SINCE_PARAMETER, Long.toString(since)).build();

            HttpResponse response = mClient.request(uri, HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                mMetrics.count(Metrics.SPLIT_CHANGES_FETCHER_STATUS_OK, 1);
                throw new IllegalStateException(exceptionMessage("http return code " + response.getHttpStatus()));
            }

            SplitChange splitChange = Json.fromJson(response.getData(), SplitChange.class);

            if(splitChange == null) {
                throw new IllegalStateException(exceptionMessage("Wrong data received from server"));
            }

            return splitChange;
        } catch (Exception e) {
            mMetrics.count(Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION, 1);
            throw new IllegalStateException(exceptionMessage(e.getLocalizedMessage()), e);
        } finally {
            mMetrics.time(Metrics.SPLIT_CHANGES_FETCHER_TIME, System.currentTimeMillis() - start);
        }
    }

    private boolean isSourceReachable() {
        return mNetworkHelper.isReachable(mTarget);
    }

    private String exceptionMessage(String message) {
        return "An unexpected has occurred while retrieving split changes: " + message;
    }

}
