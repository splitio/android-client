package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;

/**
 * Validates an instance of Key class.
 */
public class SplitNameValidator implements Validator<Split> {

    public final static int NO_ERROR = 0;
    public final static int SOME_ERROR = 1;

    private int mError = SplitNameValidator.NO_ERROR;
    private ValidationMessageLogger mMessageLogger;

    public SplitNameValidator(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidEntity(Split entity) {

        final String name = entity.name;
        mError = SplitNameValidator.SOME_ERROR;

        if (name == null) {
            mMessageLogger.e("you passed a null split name, split name must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(name)) {
            mMessageLogger.e("you passed an empty split name, split name must be a non-empty string");
            return false;
        }

        mError = SplitNameValidator.NO_ERROR;
        return true;
    }

    @Override
    public List<Integer> getWarnings() {
        return new ArrayList<>();
    }

    @Override
    public int getError() {
        return mError;
    }

    @Override
    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }
}
