package io.split.android.client.network;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpClientImpl implements HttpClient {

    Map<String, String> mHeaders;

    public HttpClientImpl() {
        mHeaders = new HashMap<>();
    }

    @Override
    public HttpRequest request(URI uri, String requestMethod) {
        return request(uri, requestMethod, null);
    }

    @Override
    public HttpRequest request(URI uri, String requestMethod, String body) {
        return new HttpRequestImpl(uri.toString(), requestMethod, body, mHeaders);
    }

    public void setHeader(String name, String value) {
        mHeaders.put(name, value);
    }


}
