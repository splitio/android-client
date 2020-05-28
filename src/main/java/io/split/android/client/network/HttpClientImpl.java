package io.split.android.client.network;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class HttpClientImpl implements HttpClient {

    private OkHttpClient mOkHttpClient;
    private Map<String, String> mHeaders;

    public HttpClientImpl() {
       this(0);
    }

    public HttpClientImpl(long readTimeout) {
        mHeaders = new HashMap<>();
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        if(readTimeout > 0) {
            okHttpClientBuilder.readTimeout(80, TimeUnit.SECONDS);
        }
        mOkHttpClient = okHttpClientBuilder.build();
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
        return new HttpStreamRequestImpl(mOkHttpClient, uri, mHeaders);
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
        // TODO: Cleanup code here
    }
}
