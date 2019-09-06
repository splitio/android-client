package fake;

import io.split.android.client.network.HttpResponse;

public class HttpResponseStub implements HttpResponse {

    public boolean isSuccess = true;
    public int code = 200;
    public String data = null;

    public HttpResponseStub(int status, boolean isSuccess) {
       this(status, isSuccess, null);
    }

    public HttpResponseStub(int status, boolean isSuccess, String data) {
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
        return null;
    }
}
