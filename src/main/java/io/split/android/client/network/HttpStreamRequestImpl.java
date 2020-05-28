package io.split.android.client.network;

import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.android.client.network.HttpMethod.GET;

public class HttpStreamRequestImpl implements HttpStreamRequest {
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient mOkHttpClient;
    private URI mUri;
    private Map<String, String> mHeaders;

    HttpStreamRequestImpl(@NonNull OkHttpClient okHttpClient, @NonNull URI uri,
                          @NonNull Map<String, String> headers) {
        mOkHttpClient = checkNotNull(okHttpClient);
        mUri = checkNotNull(uri);
        mHeaders = new HashMap<>(checkNotNull(headers));
    }

    @Override
    public HttpStreamResponse execute() throws HttpException {
        return getRequest();
    }

    @Override
    public void addHeader(String name, String value) {
        mHeaders.put(name, value);
    }

    @Override
    public void close() {
    }

    private HttpStreamResponse getRequest() throws HttpException {
        URL url;
        HttpStreamResponse response;
        try {
            url = mUri.toURL();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);
            addHeaders(requestBuilder);
            Request okHttpRequest = requestBuilder. build();
            Response okHttpResponse = mOkHttpClient.newCall(okHttpRequest).execute();
            response = buildResponse(okHttpResponse);

        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        }
        return response;
    }

    private void addHeaders(Request.Builder request) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
    }

    private HttpStreamResponse buildResponse(Response okHttpResponse) throws IOException {
        int responseCode = okHttpResponse.code();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
            return new HttpStreamResponseImpl(responseCode, new BufferedReader(new InputStreamReader(
                    okHttpResponse.body().byteStream())));
        }
        return new HttpStreamResponseImpl(responseCode);
    }
}
