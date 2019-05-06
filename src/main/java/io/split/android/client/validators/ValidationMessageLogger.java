package io.split.android.client.validators;

/**
 * Interface to implement to create a
 * logger for validations components
 * The component will be responsible to log information
 * about validation failures or warnings
 */
public interface ValidationMessageLogger {
    /**
     * Logs info related a validation fail or warning
     * @param errorInfo: Info about a failed validation result
     * @param tag: Tag for a log line
     */
    void log(ValidationErrorInfo errorInfo, String tag);
}
