package fake;

import java.net.URI;

import io.split.android.client.network.HttpMethod;
import fake.HttpStreamResponseMock;

public interface HttpResponseMockDispatcher {
    HttpResponseMock getResponse(URI uri, HttpMethod method, String body);

    HttpStreamResponseMock getStreamResponse(URI uri);
}
