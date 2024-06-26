package io.split.android.client.service.http;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.logger.Logger;

public class HttpFetcherImpl<T> implements HttpFetcher<T> {

    public static final String TILL_PARAM = "till";
    private final HttpClient mClient;
    private final URI mTarget;
    private final HttpResponseParser<T> mResponseParser;

    public HttpFetcherImpl(@NonNull HttpClient client,
                           @NonNull URI target,
                           @NonNull HttpResponseParser<T> responseParser) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mResponseParser = checkNotNull(responseParser);
    }

    @Override
    public T execute(@NonNull Map<String, Object> params,
                     @Nullable Map<String, String> headers) throws HttpFetcherException {
        checkNotNull(params);
        T responseData;

        try {
            URIBuilder uriBuilder = new URIBuilder(mTarget);
            if (params.containsKey(TILL_PARAM)) {
                Object till = params.remove(TILL_PARAM);
                params.put(TILL_PARAM, till);
            }

            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                uriBuilder.addParameter(param.getKey(), value != null ? value.toString() : "");
            }
            URI builtUri = uriBuilder.build();
            HttpResponse response = mClient.request(builtUri, HttpMethod.GET, null, headers).execute();
            if (builtUri != null && response != null) {
                Logger.v("Received from: " + builtUri + " -> " + response.getData());
            }
            if (!response.isSuccess()) {
                int httpStatus = response.getHttpStatus();
                throw new HttpFetcherException(mTarget.toString(), "http return code " + httpStatus, httpStatus);
            }

            responseData = mResponseParser.parse(response.getData());

            if (responseData == null) {
                throw new IllegalStateException("Wrong data received from split changes server");
            }
        } catch (HttpFetcherException httpFetcherException) {
            throw httpFetcherException;
        } catch (HttpException e) {
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage(), e.getStatusCode());
        } catch (Exception e) {
            throw new HttpFetcherException(mTarget.toString(), e.getLocalizedMessage());
        }
        return responseData;
    }
}
