package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.utils.Logger;

/**
 * Default implementation of ValidationMessageLogger interface
 */
public class ValidationMessageLoggerImpl implements ValidationMessageLogger {

    @Override
    public void log(ValidationErrorInfo errorInfo, String tag) {
        if(errorInfo.isError() && errorInfo.getErrorMessage() != null) {
            e(errorInfo, tag);
        } else {
            w(errorInfo, tag);
        }
    }

    @Override
    public void e(ValidationErrorInfo errorInfo, String tag) {
        e(tag, errorInfo.getErrorMessage());
    }

    @Override
    public void w(ValidationErrorInfo errorInfo, String tag) {
        List<String> warnings = new ArrayList<>(errorInfo.getWarnings().values());
        for(String warning : warnings) {
            w(tag, warning);
        }
    }

    public void e(String message, String tag) {
        logError(message, tag);
    }

    public void w(String message, String tag) {
        logWarning(message, tag);
    }

    private void logError(String message, String tag) {
        Logger.e(sanitizeTag(tag) + ": " + message);
    }

    private void logWarning(String message, String tag) {
        Logger.w(sanitizeTag(tag) + ": " + message);
    }

    private String sanitizeTag(String tag) {
        return (tag != null ? tag : "");
    }

}
