package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;

/**
 * Validates an instance of Key class.
 */
public class ApiKeyValidator implements Validator<ApiKeyValidatable> {

    public final static int NO_ERROR = 0;
    public final static int SOME_ERROR = 1;

    private ValidationMessageLogger mMessageLogger;
    private int mError = NO_ERROR;

    public ApiKeyValidator(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidEntity(ApiKeyValidatable entity) {
        mError = SOME_ERROR;
        final String apiKey = entity.getApiKey();

        if (apiKey == null) {
            mMessageLogger.e("you passed a null api_key, the api_key must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(apiKey)) {
            mMessageLogger.e("you passed and empty api_key, api_key must be a non-empty string");
            return false;
        }

        mError = NO_ERROR;
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
