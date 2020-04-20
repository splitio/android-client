package io.split.android.client.network;

public abstract class BaseHttpResponseImpl implements BaseHttpResponse {
private static final int HTTP_OK = 200;
    protected static final int HTTP_MULTIPLE_CHOICES = 300;
    protected static final int HTTP_UNAUTHORIZED = 401;
    protected static final int HTTP_BAD_REQUEST = 400;

    private int mHttpStatus;

    BaseHttpResponseImpl(int httpStatus) {
        mHttpStatus = httpStatus;
    }

    @Override
    public boolean isSuccess() {
        return mHttpStatus >= HTTP_OK && mHttpStatus< HTTP_MULTIPLE_CHOICES;
    }

    @Override
    public boolean isCredentialsError() {
        return mHttpStatus == HTTP_UNAUTHORIZED;
    }

    @Override
    public boolean isBadRequestError() {
        return mHttpStatus == HTTP_BAD_REQUEST;
    }

    @Override
    public int getHttpStatus() {
        return mHttpStatus;
    }
}
