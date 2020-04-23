package fake;

import java.io.BufferedReader;
import java.io.IOException;

import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;

public class HttpStreamRequestMock implements HttpStreamRequest {

    private final HttpStreamResponse mResponse;

    public HttpStreamRequestMock(HttpStreamResponse response) {
        mResponse = response;
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public HttpStreamResponse execute() throws HttpException {
        return mResponse;
    }

    @Override
    public void close() {
        HttpStreamResponseMock mockedResponse = (HttpStreamResponseMock )mResponse;
        mockedResponse.close();
    }
}
