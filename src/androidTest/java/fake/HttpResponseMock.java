package fake;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.network.BaseHttpResponseImpl;
import io.split.android.client.network.HttpResponse;

@SuppressWarnings("ConstantConditions")
public class HttpResponseMock extends BaseHttpResponseImpl implements HttpResponse {

    private String data;

    public HttpResponseMock(int status) {
        this(status, null);
    }

    public HttpResponseMock(int status, String data) {
        super(status);
        this.data = data;
    }

    @Override
    public String getData() {
        return data;
    }
}
