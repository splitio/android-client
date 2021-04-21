package io.split.android.client.network;

import java.net.URI;
import java.util.Map;

public interface HttpClient {
    void setHeader(String name, String value);

    void addHeaders(Map<String, String> headers);

    HttpRequest request(URI uri, HttpMethod httpMethod);

    HttpRequest request(URI uri, HttpMethod requestMethod, String body, Map<String, String> headers);

    HttpRequest request(URI uri, HttpMethod httpMethod, String body);

    HttpStreamRequest streamRequest(URI uri);

    void close();
}
