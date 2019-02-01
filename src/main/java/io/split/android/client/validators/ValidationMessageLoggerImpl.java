package io.split.android.client.validators;

import io.split.android.client.utils.Logger;

/**
 * Default implementation of ValidationMessageLogger interface
 */
class ValidationMessageLoggerImpl implements ValidationMessageLogger {

    private String mTag;

    public ValidationMessageLoggerImpl(String tag) {
        this.mTag = tag != null ?  tag : "";
    }

    @Override
    public void setTag(String tag) {
        mTag = tag;
    }

    @Override
    public void e(String message) {
        Logger.e(mTag + ": " + message);
    }

    @Override
    public void w(String message) {
        Logger.w(mTag + ": " + message);
    }
}
