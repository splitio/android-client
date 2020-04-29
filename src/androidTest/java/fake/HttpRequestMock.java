package fake;

import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;

public class HttpRequestMock implements HttpRequest {
    final private HttpResponse mResponse;

    public HttpRequestMock(HttpResponse response) {
        mResponse = response;
    }

    @Override
    public HttpResponse execute() throws HttpException {
        return mResponse;
    }
}
