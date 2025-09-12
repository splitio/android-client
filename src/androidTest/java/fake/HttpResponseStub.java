package fake;

import java.security.cert.Certificate;

import io.split.android.client.network.BaseHttpResponseImpl;
import io.split.android.client.network.HttpResponse;

@SuppressWarnings("ConstantConditions")
public class HttpResponseStub extends BaseHttpResponseImpl implements HttpResponse {


    private boolean isSuccess;
    private String data;

    public HttpResponseStub(int status, boolean isSuccess) {
        this(status, isSuccess, null);
    }

    private HttpResponseStub(int status, boolean isSuccess, String data) {
        super(status);
        this.isSuccess = isSuccess;
        this.data = data;
    }

    @Override
    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public Certificate[] getServerCertificates() {
        return new Certificate[0];
    }
}
