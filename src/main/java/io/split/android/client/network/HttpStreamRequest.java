package io.split.android.client.network;

import java.io.IOException;

public interface HttpStreamRequest {
    void addHeader(String name, String value);
    HttpStreamResponse execute() throws HttpException, IOException;
    void close();
}
