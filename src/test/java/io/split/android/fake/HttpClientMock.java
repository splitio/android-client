package io.split.android.fake;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpRequest;

public class HttpClientMock implements HttpClient {
    @Override
    public void setHeader(String name, String value) {

    }

    @Override
    public void addHeaders(Map<String, String> headers) {

    }

    @Override
    public HttpRequest request(URI uri, String httpMethod) {
        return null;
    }

    @Override
    public HttpRequest request(URI uri, String httpMethod, String body) {
        return null;
    }

    @Override
    public void close() {

    }
}
