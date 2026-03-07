package io.split.android.client.tracker;

/**
 * Logging abstraction for the tracker module.
 */
public interface TrackerLogger {
    /** Log a validation result (error or warning) with a tag. */
    void log(TrackerValidationError errorInfo, String tag);

    /** Log an error message with a tag. */
    void e(String message, String tag);

    /** Log a verbose message. */
    void v(String message);
}
