package io.split.android.client.service.http;

import androidx.annotation.NonNull;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.NetworkHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpSseAuthTokenFetcher<T> implements HttpFetcher<SseAuthenticationResponse> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final NetworkHelper mNetworkHelper;
    private HttpResponseParser<SseAuthenticationResponse> mResponseParser;

    public HttpSseAuthTokenFetcher(@NonNull HttpClient client,
                                   @NonNull URI target,
                                   @NonNull NetworkHelper networkHelper,
                                   @NonNull HttpResponseParser<SseAuthenticationResponse> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mNetworkHelper = checkNotNull(networkHelper);
        mResponseParser = checkNotNull(responseParser);
    }

    @Override
    public SseAuthenticationResponse execute(@NonNull Map<String, Object> params) throws HttpFetcherException {
        checkNotNull(params);
        SseAuthenticationResponse responseData = null;

        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }

            URIBuilder uriBuilder = new URIBuilder(mTarget);
            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                uriBuilder.addParameter(param.getKey(), value != null ? value.toString() : "");
            }

            HttpResponse response = mClient.request(uriBuilder.build(), HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                if (response.isCredentialsError()) {
                    return new SseAuthenticationResponse(false);
                }
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
