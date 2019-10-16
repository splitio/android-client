package io.split.android.client.validators;

import com.google.common.base.Strings;

/**
 * Validates an instance of Key class.
 */
public class KeyValidatorImpl implements KeyValidator {

    private final int MAX_MATCHING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();
    private final int MAX_BUCKETING_KEY_LENGTH = ValidationConfig.getInstance().getMaximumKeyLength();

    @Override
    public ValidationErrorInfo validate(String matchingKey, String bucketingKey) {

        if (matchingKey == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null key, matching key must be a non-empty string");
        }

        if (Strings.isNullOrEmpty(matchingKey.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME,"you passed an empty string, matching key must be a non-empty string");
        }

        if (matchingKey.length() > MAX_MATCHING_KEY_LENGTH) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "matching key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
        }

        if (bucketingKey != null) {
            if (Strings.isNullOrEmpty(bucketingKey.trim())) {
                return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty string, bucketing key must be null or a non-empty string");
            }

            if (bucketingKey.length() > MAX_BUCKETING_KEY_LENGTH) {
                return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "bucketing key too long - must be " + MAX_MATCHING_KEY_LENGTH + " characters or less");
            }
        }
        return null;
    }
}
