package io.split.android.client.validators;

import com.google.common.base.Strings;

public class ApiKeyValidatorImpl implements ApiKeyValidator {

    @Override
    public ValidationErrorInfo validate(String sdkKey) {

        if (sdkKey == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null sdkKey, the sdkKey must be a non-empty string");
        }

        if (Strings.isNullOrEmpty(sdkKey.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty sdkKey, sdkKey must be a non-empty string");
        }

        return null;
    }
}
