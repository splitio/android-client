package io.split.android.client.service.http;

import static com.google.common.base.Preconditions.checkNotNull;

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

public class HttpFetcherImpl<T> implements HttpFetcher<T> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final NetworkHelper mNetworkHelper;
    private HttpResponseParser<T> mResponseParser;

    public HttpFetcherImpl(@NonNull HttpClient client,
                           @NonNull URI target,
                           @NonNull NetworkHelper networkHelper,
                           @NonNull HttpResponseParser<T> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mNetworkHelper = checkNotNull(networkHelper);
        mResponseParser = checkNotNull(responseParser);
    }

    @Override
    public T execute(@NonNull Map<String, Object> params,
                     @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        T responseData;
        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }
            URIBuilder uriBuilder = new URIBuilder(mTarget);
            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                uriBuilder.addParameter(param.getKey(), value != null ? value.toString() : "");
            }
            URI builtUri = uriBuilder.build();
            HttpResponse response = mClient.request(builtUri, HttpMethod.GET, null, headers).execute();
            Logger.d("Received from: " + builtUri.toString() + " -> " + response.getData());
            if (!response.isSuccess()) {
                throw new IllegalStateException("http return code " + response.getHttpStatus());
            }

            responseData = mResponseParser.parse(response.getData());

            if (responseData == null) {
                throw new IllegalStateException("Wrong data received from split changes server");
            }
        } catch (Exception e) {
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage());
        }
        return responseData;
    }
}
