package io.split.android.client.network;

public class HttpResponseImpl extends BaseHttpResponseImpl implements  HttpResponse {

    private String mData;

    HttpResponseImpl(int httpStatus) {
        this(httpStatus, null);
    }

    public HttpResponseImpl(int httpStatus, String data) {
        super(httpStatus);
        mData = data;
    }

    @Override
    public String getData() {
        return mData;
    }
}
