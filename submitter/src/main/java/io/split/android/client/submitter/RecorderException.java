package io.split.android.client.submitter;

public class RecorderException extends Exception {
    private final Integer mHttpStatus;
    private final boolean mRetryable;

    public RecorderException(String message, Integer httpStatus, boolean retryable) {
        super(message);
        this.mHttpStatus = httpStatus;
        this.mRetryable = retryable;
    }

    public Integer getHttpStatus() {
        return mHttpStatus;
    }

    public boolean isRetryable() {
        return mRetryable;
    }
}
