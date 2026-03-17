package io.split.android.client.tracker;

import java.util.Collections;
import java.util.List;

/**
 * Simple error/warning result from tracker validation.
 */
public class TrackerValidationError {
    private final boolean mIsError;
    private final String mMessage;
    private final List<String> mWarnings;

    public TrackerValidationError(boolean isError, String message) {
        mIsError = isError;
        mMessage = message;
        mWarnings = Collections.emptyList();
    }

    public TrackerValidationError(List<String> warnings) {
        mIsError = false;
        mMessage = null;
        mWarnings = (warnings != null) ? warnings : Collections.<String>emptyList();
    }

    public boolean isError() {
        return mIsError;
    }

    public String getMessage() {
        return mMessage;
    }

    public List<String> getWarnings() {
        return mWarnings;
    }
}
