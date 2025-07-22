package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.security.cert.Certificate;

public class HttpStreamResponseImpl extends BaseHttpResponseImpl implements HttpStreamResponse {

    private final BufferedReader mData;

    HttpStreamResponseImpl(int httpStatus) {
        this(httpStatus, null);
    }

    public HttpStreamResponseImpl(int httpStatus, BufferedReader data) {
        this(httpStatus, data, new Certificate[]{});
    }

    public HttpStreamResponseImpl(int httpStatus, BufferedReader data, Certificate[] serverCertificates) {
        super(httpStatus);
        mData = data;
    }

    @Override
    @Nullable
    public BufferedReader getBufferedReader() {
        return mData;
    }
}
