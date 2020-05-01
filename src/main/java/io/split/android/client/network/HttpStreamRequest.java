package io.split.android.client.network;

public interface HttpStreamRequest {
    void addHeader(String name, String value);
    HttpStreamResponse execute() throws HttpException;
    void close();
}
