package io.split.android.client.validators;

import com.google.common.base.Strings;

public class ApiKeyValidatorImpl implements ApiKeyValidator {

    @Override
    public ValidationErrorInfo validate(String apiKey) {

        if (apiKey == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null api_key, the api_key must be a non-empty string");
        }

        if (Strings.isNullOrEmpty(apiKey.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty api_key, api_key must be a non-empty string");
        }

        return null;
    }
}
