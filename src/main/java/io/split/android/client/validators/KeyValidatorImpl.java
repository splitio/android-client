package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;

/**
 * Validates an instance of Key class.
 */
public class KeyValidatorImpl implements KeyValidator {

    private ValidationMessageLogger mMessageLogger;

    private final int MAX_MATCHING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();
    private final int MAX_BUCKETING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();

    public KeyValidatorImpl() {
        this.mMessageLogger = new ValidationMessageLoggerImpl();
    }

    @Override
    public boolean isValidKey(String matchingKey, String bucketingKey, String logTag) {

        if (matchingKey == null) {
            mMessageLogger.e(logTag, "you passed a null key, matching key must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(matchingKey)) {
            mMessageLogger.e(logTag,"you passed an empty string, matching key must be a non-empty string");
            return false;
        }

        if (matchingKey.length() > MAX_MATCHING_KEY_LENGTH) {
            mMessageLogger.e(logTag,"matching key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
            return false;
        }

        if (bucketingKey != null) {
            if (bucketingKey.trim() == "") {
                mMessageLogger.e(logTag,"you passed an empty string, bucketing key must be null or a non-empty string");
                return false;
            }

            if (bucketingKey.length() > MAX_BUCKETING_KEY_LENGTH) {
                mMessageLogger.e(logTag,"bucketing key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
                return false;
            }
        }
        return true;
    }

    @Override
    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }

}
