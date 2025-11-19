package io.split.android.client.network;

import androidx.annotation.Nullable;

public class HttpException extends Exception {

    private final Integer mStatusCode;

    public HttpException(String message) {
        super("HttpException: " + message);
        mStatusCode = null;
    }

    public HttpException(String message, int statusCode) {
        super("HttpException: " + message);
        mStatusCode = statusCode;
    }

    @Nullable
    public Integer getStatusCode() {
        return mStatusCode;
    }
}
