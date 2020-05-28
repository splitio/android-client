package io.split.android.client.network;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class HttpClientImpl implements HttpClient {
    private static final long STREAMING_CONNECTION_TIMEOUT_IN_SECONDS = 80;
    private OkHttpClient mOkHttpClient;
    private OkHttpClient mOkHttpClientStreaming;
    private Map<String, String> mHeaders;

    public HttpClientImpl() {
        mHeaders = new HashMap<>();
        mOkHttpClient = new OkHttpClient();
        mOkHttpClientStreaming  = new OkHttpClient.Builder()
                .readTimeout(STREAMING_CONNECTION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod) {
        return request(uri, requestMethod, null);
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body) {
        return new HttpRequestImpl(mOkHttpClient, uri, requestMethod, body, mHeaders);
    }

    @Override
    public HttpStreamRequest streamRequest(URI uri) {
        return new HttpStreamRequestImpl(mOkHttpClientStreaming, uri, mHeaders);
    }

    @Override
    public void setHeader(String name, String value) {
        if(name == null || value == null) {
            throw new IllegalArgumentException(String.format("Invalid value for header %s: %s", name, value));
        }
        mHeaders.put(name, value);
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
        for(Map.Entry<String, String> header : headers.entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
    }

    @Override
    public void close() {
        mOkHttpClient.connectionPool().evictAll();
        mOkHttpClientStreaming.connectionPool().evictAll();
    }
}
