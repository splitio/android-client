package io.split.android.client.service.sseclient.spi;

import androidx.annotation.Nullable;

/**
 * Exception thrown by streaming auth fetchers.
 */
public class StreamingAuthException extends Exception {

    @Nullable
    private final Integer mStatusCode;

    public StreamingAuthException(String message) {
        super(message);
        mStatusCode = null;
    }

    public StreamingAuthException(String message, Throwable cause) {
        super(message, cause);
        mStatusCode = null;
    }

    public StreamingAuthException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        mStatusCode = statusCode;
    }

    @Nullable
    public Integer getStatusCode() {
        return mStatusCode;
    }
}
