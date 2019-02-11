package io.split.android.client.validators;

import com.google.common.base.Strings;

public class ApiKeyValidatorImpl implements ApiKeyValidator {

    private ValidationMessageLogger mMessageLogger;

    public ApiKeyValidatorImpl(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidApiKey(String apiKey) {

        if (apiKey == null) {
            mMessageLogger.e("you passed a null api_key, the api_key must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(apiKey)) {
            mMessageLogger.e("you passed an empty api_key, api_key must be a non-empty string");
            return false;
        }

        return true;
    }

    @Override
    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }

}
