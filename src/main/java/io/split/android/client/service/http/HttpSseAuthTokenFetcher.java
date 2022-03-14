package io.split.android.client.service.http;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.NetworkHelper;

public class HttpSseAuthTokenFetcher implements HttpFetcher<SseAuthenticationResponse> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final NetworkHelper mNetworkHelper;
    private final HttpResponseParser<SseAuthenticationResponse> mResponseParser;

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
    public SseAuthenticationResponse execute(@NonNull Map<String, Object> params,
                                             @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        SseAuthenticationResponse responseData;

        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }
            URI build = getUri(params, mTarget);
            HttpResponse response = mClient.request(build, HttpMethod.GET).execute();

            if (!response.isSuccess()) {
                if (response.isClientRelatedError()) {
                    return new SseAuthenticationResponse(true);
                }
                throw new IllegalStateException("http return code " + response.getHttpStatus());
            }

            responseData = mResponseParser.parse(response.getData());

            if (responseData == null) {
                throw new IllegalStateException("Wrong data received from authentication server");
            }
        } catch (Exception e) {
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage());
        }
        return responseData;
    }

    private static URI getUri(Map<String, Object> params, URI target) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(target);
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (param.getValue() instanceof Iterable) {
                for (Object paramValue : ((Iterable<Object>) param.getValue())) {
                    uriBuilder.addParameter(param.getKey(), paramValue.toString());
                }
            } else {
                uriBuilder.addParameter(param.getKey(), String.valueOf(param.getValue()));
            }
        }

        return uriBuilder.build();
    }
}
