package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;

/**
 * Implementation of split validation interface
 */
public class SplitValidatorImpl implements SplitValidator {

    private ValidationMessageLogger mMessageLogger;

    public SplitValidatorImpl() {
        this("");
    }

    public SplitValidatorImpl(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidName(final String name) {

        if (name == null) {
            mMessageLogger.e("you passed a null split name, split name must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(name)) {
            mMessageLogger.e("you passed an empty split name, split name must be a non-empty string");
            return false;
        }

        if (nameHasToBeTrimmed(name)){
            mMessageLogger.w("split name '" + name + "' has extra whitespace, trimming");
        }
        return true;
    }

    @Override
    public boolean nameHasToBeTrimmed(String name) {
        return name.trim().length() != name.length();
    }

    @Override
    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }

    @Override
    public void setTag(String tag) {
        mMessageLogger.setTag(tag);
    }
}
