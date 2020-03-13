package fake;

import io.split.android.client.network.HttpResponse;

@SuppressWarnings("ConstantConditions")
public class HttpResponseStub implements HttpResponse {

    private boolean isSuccess;
    private int code = 200;
    private String data;

    public HttpResponseStub(int status, boolean isSuccess) {
       this(status, isSuccess, null);
    }

    private HttpResponseStub(int status, boolean isSuccess, String data) {
        this.code = code;
        this.isSuccess = isSuccess;
        this.data = data;
    }

    @Override
    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public int getHttpStatus() {
        return code;
    }

    @Override
    public String getData() {
        return data;
    }
}
