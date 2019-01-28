package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;

/**
 * Validates an instance of Key class.
 */
public class KeyValidator implements Validator<Key> {

    public final static int NO_ERROR = 0;
    public final static int ERROR_NULL_MATCHING_KEY = 1;
    public final static int ERROR_EMPTY_MATCHING_KEY = 2;
    public final static int ERROR_LONG_MATCHING_KEY = 3;
    public final static int ERROR_EMPTY_BUCKETING_KEY = 5;
    public final static int ERROR_LONG_BUCKETING_KEY = 5;

    private int mError = KeyValidator.NO_ERROR;
    private ValidationMessageLogger mMessageLogger;

    private final int MAX_MATCHING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();
    private final int MAX_BUCKETING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();

    public KeyValidator(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidEntity(Key entity) {

        final String matchingKey = entity.matchingKey();
        final String bucketingKey = entity.bucketingKey();

        if (matchingKey == null) {
            mMessageLogger.e("you passed a null key, matching key must be a non-empty string");
            mError = KeyValidator.ERROR_NULL_MATCHING_KEY;
            return false;
        }

        if (Strings.isNullOrEmpty(matchingKey)) {
            mMessageLogger.e("you passed an empty string, matching key must be a non-empty string");
            mError = KeyValidator.ERROR_EMPTY_MATCHING_KEY;
            return false;
        }

        if (matchingKey.length() > MAX_MATCHING_KEY_LENGTH) {
            mMessageLogger.e("matching key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
            mError = KeyValidator.ERROR_LONG_MATCHING_KEY;
            return false;
        }

        if (bucketingKey != null)

        {
            if (bucketingKey.trim() == "") {
                mMessageLogger.e("you passed an empty string, bucketing key must be null or a non-empty string");
                mError = KeyValidator.ERROR_EMPTY_BUCKETING_KEY;
                return false;
            }

            if (bucketingKey.length() > MAX_BUCKETING_KEY_LENGTH) {
                mMessageLogger.e("bucketing key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
                mError = KeyValidator.ERROR_LONG_BUCKETING_KEY;
                return false;
            }
        }
        mError = KeyValidator.NO_ERROR;
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
