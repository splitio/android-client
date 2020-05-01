package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;

public class HttpStreamResponseImpl extends BaseHttpResponseImpl implements HttpStreamResponse {

    private BufferedReader mData;

    HttpStreamResponseImpl(int httpStatus) {
        this(httpStatus, null);
    }

    public HttpStreamResponseImpl(int httpStatus, BufferedReader data) {
        super(httpStatus);
        mData = data;
    }

    @Override
    @Nullable
    public BufferedReader getBufferedReader() {
        return mData;
    }
}
