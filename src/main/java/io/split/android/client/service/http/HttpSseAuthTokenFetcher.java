package io.split.android.client.service.http;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.logger.Logger;

public class HttpSseAuthTokenFetcher implements HttpFetcher<SseAuthenticationResponse> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final HttpResponseParser<SseAuthenticationResponse> mResponseParser;

    public HttpSseAuthTokenFetcher(@NonNull HttpClient client,
                                   @NonNull URI target,
                                   @NonNull HttpResponseParser<SseAuthenticationResponse> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mResponseParser = checkNotNull(responseParser);
    }

    @Override
    public SseAuthenticationResponse execute(@NonNull Map<String, Object> params,
                                             @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        SseAuthenticationResponse responseData;

        try {
            URI build = getUri(params, mTarget);
            HttpResponse response = mClient.request(build, HttpMethod.GET).execute();
            if (build != null && response != null) {
                Logger.v("Received from: " + build.toString() + " -> " + response.getData());
            }
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
