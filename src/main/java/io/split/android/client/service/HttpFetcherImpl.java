package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.metrics.FetcherMetricsConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpFetcherImpl<T> implements HttpFetcher<T> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final Metrics mMetrics;
    private final FetcherMetricsConfig mFetcherMetricsConfig;
    private final NetworkHelper mNetworkHelper;
    private HttpResponseParser<T> mResponseParser;

    public HttpFetcherImpl(@NonNull HttpClient client,
                           @NonNull URI target,
                           @NonNull Metrics metrics,
                           @NonNull FetcherMetricsConfig fetcherMetricsConfig,
                           @NonNull NetworkHelper networkHelper,
                           @NonNull HttpResponseParser<T> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mMetrics = checkNotNull(metrics);
        mNetworkHelper = checkNotNull(networkHelper);
        mFetcherMetricsConfig = checkNotNull(fetcherMetricsConfig);
        mResponseParser = checkNotNull(responseParser);
    }

    @Override
    public T execute(Map<String, Object> params) throws HttpFetcherException {

        long start = System.currentTimeMillis();
        T responseData = null;

        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }

            URIBuilder uriBuilder = new URIBuilder(mTarget);
            if (params != null) {
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    uriBuilder.addParameter(param.getKey(), param.getValue().toString());
                }
            }

            HttpResponse response = mClient.request(uriBuilder.build(), HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                mMetrics.count(String.format(mFetcherMetricsConfig.getStatusLabel(), response.getHttpStatus()), 1);
                throw new IllegalStateException("http return code " + response.getHttpStatus());
            }

            responseData = mResponseParser.parse(response.getData());

            if (responseData == null) {
                throw new IllegalStateException("Wrong data received from split changes server");
            }
        } catch (Exception e) {
            mMetrics.count(mFetcherMetricsConfig.getExceptionLabel(), 1);
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage());
        } finally {
            mMetrics.time(mFetcherMetricsConfig.getTimeLabel(), System.currentTimeMillis() - start);
        }
        return responseData;
    }
}
