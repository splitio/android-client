package io.split.android.client.tracker;

/**
 * Simple error/warning result from tracker validation.
 */
public class TrackerValidationError {
    private final boolean mIsError;
    private final String mMessage;

    public TrackerValidationError(boolean isError, String message) {
        mIsError = isError;
        mMessage = message;
    }

    public boolean isError() {
        return mIsError;
    }

    public String getMessage() {
        return mMessage;
    }
}
