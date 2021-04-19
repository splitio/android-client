package io.split.android.client.service.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.metrics.FetcherMetricsConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpFetcherImpl<T> implements HttpFetcher<T> {

    private final HttpClient mClient;
    private final URI mTarget;
    private Metrics mMetrics;
    private FetcherMetricsConfig mFetcherMetricsConfig;
    private final NetworkHelper mNetworkHelper;
    private HttpResponseParser<T> mResponseParser;

    public HttpFetcherImpl(@NonNull HttpClient client,
                           @NonNull URI target,
                           @NonNull NetworkHelper networkHelper,
                           @NonNull HttpResponseParser<T> responseParser) {
        this(client, target, null, null, networkHelper, responseParser);
    }

    public HttpFetcherImpl(@NonNull HttpClient client,
                           @NonNull URI target,
                           Metrics metrics,
                           FetcherMetricsConfig fetcherMetricsConfig,
                           @NonNull NetworkHelper networkHelper,
                           @NonNull HttpResponseParser<T> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mNetworkHelper = checkNotNull(networkHelper);
        mResponseParser = checkNotNull(responseParser);
        mMetrics = metrics;
        mFetcherMetricsConfig = fetcherMetricsConfig;
        if(metrics != null) {
            checkNotNull(fetcherMetricsConfig);
        }
    }

    @Override
    public T execute(@NonNull Map<String, Object> params,
                     @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        long start = System.currentTimeMillis();
        T responseData = null;
        Logger.d("[REMOVE]  FETCH EXEC");
        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                Logger.d("[REMOVE]  SUPER ERROR HERE: Source not reachable");
                throw new IllegalStateException("Source not reachable");
            }
            Logger.d("[REMOVE]  SUPER ERROR HERE: AFTERRRRR Source not reachable");
            URIBuilder uriBuilder = new URIBuilder(mTarget);
            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                uriBuilder.addParameter(param.getKey(), value != null ? value.toString() : "");
            }
            URI u = uriBuilder.build();
            Logger.d("[REMOVE]  AFETER BUILDER");
            HttpResponse response = mClient.request(uriBuilder.build(), HttpMethod.GET, null, headers).execute();
            Logger.d("[REMOVE]  AFETER RESPONSE");
            Logger.d("Received from: " + u.toString() + " -> " + response.getData());
            if (!response.isSuccess()) {
                if(mMetrics != null) {
                    mMetrics.count(String.format(mFetcherMetricsConfig.getStatusLabel(), response.getHttpStatus()), 1);
                }
                throw new IllegalStateException("http return code " + response.getHttpStatus());
            }

            responseData = mResponseParser.parse(response.getData());

            if (responseData == null) {
                throw new IllegalStateException("Wrong data received from split changes server");
            }
        } catch (Exception e) {
            Logger.d("[REMOVE]  SUPER ERROR HERE EXCEIPTIO: " + e.getLocalizedMessage());
            if(mMetrics != null) {
                mMetrics.count(mFetcherMetricsConfig.getExceptionLabel(), 1);
            }
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage());
        } finally {
            Logger.d("[REMOVE]  SUPER FINALLLY: ");
            if(mMetrics != null) {
                mMetrics.time(mFetcherMetricsConfig.getTimeLabel(), System.currentTimeMillis() - start);
            }
        }
        return responseData;
    }
}
