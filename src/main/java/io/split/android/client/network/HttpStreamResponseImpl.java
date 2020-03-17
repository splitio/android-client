package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;

public class HttpStreamResponseImpl implements HttpStreamResponse {

    private int mHttpStatus;
    private BufferedReader mData;

    HttpStreamResponseImpl(int httpStatus) {
        mHttpStatus = httpStatus;
    }

    public HttpStreamResponseImpl(int httpStatus, BufferedReader data) {
        mHttpStatus = httpStatus;
        mData = data;
    }

    @Override
    public boolean isSuccess() {
        return mHttpStatus >= 200 && mHttpStatus < 300;
    }

    @Override
    public int getHttpStatus() {
        return mHttpStatus;
    }

    @Override
    @Nullable
    public BufferedReader getBufferedReader() {
        return mData;
    }
}
