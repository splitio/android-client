package io.split.android.client.network;

import java.net.URI;

public interface HttpClient {
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    HttpRequest request(URI uri, String httpMethod);
    HttpRequest request(URI uri, String httpMethod, String body);
}
