package fake;

import java.net.URI;

import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpStreamResponse;

public interface HttpResponseMockDispatcher {
    HttpResponseMock getResponse(URI uri, HttpMethod method, String body);

    HttpStreamResponseMock getStreamResponse(URI uri);
}
