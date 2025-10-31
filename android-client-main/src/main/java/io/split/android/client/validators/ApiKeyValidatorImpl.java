package io.split.android.client.validators;

import io.split.android.client.utils.Utils;

public class ApiKeyValidatorImpl implements ApiKeyValidator {

    @Override
    public ValidationErrorInfo validate(String sdkKey) {

        if (sdkKey == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null sdkKey, the sdkKey must be a non-empty string");
        }

        if (Utils.isNullOrEmpty(sdkKey.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty sdkKey, sdkKey must be a non-empty string");
        }

        return null;
    }
}
