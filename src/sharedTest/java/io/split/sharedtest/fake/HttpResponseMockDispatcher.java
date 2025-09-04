package io.split.sharedtest.fake;

import java.net.URI;

import io.split.android.client.network.HttpMethod;

public interface HttpResponseMockDispatcher {
    HttpResponseMock getResponse(URI uri, HttpMethod method, String body);

    HttpStreamResponseMock getStreamResponse(URI uri);
}
