package io.split.android.client.network;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpClientImpl implements HttpClient {

    private Map<String, String> mHeaders;

    public HttpClientImpl() {
        mHeaders = new HashMap<>();
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod) {
        return request(uri, requestMethod, null);
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body) {
        return new HttpRequestImpl(uri, requestMethod, body, mHeaders);
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
