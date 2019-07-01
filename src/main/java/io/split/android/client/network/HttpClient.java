package io.split.android.client.network;

import java.net.URI;
import java.util.Map;

public interface HttpClient {
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    void setHeader(String name, String value);
    void addHeaders(Map<String, String> headers);
    HttpRequest request(URI uri, String httpMethod);
    HttpRequest request(URI uri, String httpMethod, String body);
    void close();
}
