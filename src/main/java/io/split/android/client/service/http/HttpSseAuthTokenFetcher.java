package io.split.android.client.service.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.NetworkHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpSseAuthTokenFetcher implements RepeatableParameterHttpFetcher<SseAuthenticationResponse> {

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
    public SseAuthenticationResponse execute(@NonNull Set<Pair<String, Object>> params,
                                             @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        SseAuthenticationResponse responseData;

        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }
            URI build = getUri(params);
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

    private URI getUri(Set<Pair<String, Object>> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(mTarget);
        for (Pair<String, Object> param : params) {
            if (param.first != null && param.second != null) {
                uriBuilder.addParameter(param.first, param.second.toString());
            }
        }

        return uriBuilder.build();
    }
}
