package io.split.sharedtest.fake;

import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;

public class HttpRequestStub implements HttpRequest {
    @Override
    public HttpResponse execute() throws HttpException {
        return new HttpResponseStub(200, true);
    }
}
