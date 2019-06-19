package io.split.android.client.network;

public class HttpResponseImpl implements HttpResponse {

    private int mRequestStatus;
    private int mHttpStatus;
    private String mData;

    HttpResponseImpl(int requestStatus, int httpStatus) {
        mRequestStatus = requestStatus;
        mHttpStatus = httpStatus;
    }

    HttpResponseImpl(int requestStatus, int httpStatus, String data) {
        mRequestStatus = requestStatus;
        mHttpStatus = httpStatus;
        mData = data;
    }

    @Override
    public int getRequestStatus() {
        return mRequestStatus;
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
