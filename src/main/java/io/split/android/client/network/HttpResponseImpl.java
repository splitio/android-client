package io.split.android.client.network;

public class HttpResponseImpl implements HttpResponse {

    private int mHttpStatus;
    private String mData;

    HttpResponseImpl(int httpStatus) {
        mHttpStatus = httpStatus;
    }

    public HttpResponseImpl(int httpStatus, String data) {
        mHttpStatus = httpStatus;
        mData = data;
    }

    @Override
    public boolean isSuccess() {
        return mHttpStatus >= 200 && mHttpStatus< 300;
    }

    @Override
    public int getHttpStatus() {
        return mHttpStatus;
    }

    @Override
    public String getData() {
        return mData;
    }
}
