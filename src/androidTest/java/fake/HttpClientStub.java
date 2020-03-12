package fake;

import java.net.URI;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpStreamRequest;

public class HttpClientStub implements HttpClient {
    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod httpMethod) {
        return new HttpRequestStub();
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod httpMethod, String body) {
        return new HttpRequestStub();
    }

    @Override
    public HttpStreamRequest streamRequest(URI uri) {
        return null;
    }

    @Override
    public void close() {
    }
}
