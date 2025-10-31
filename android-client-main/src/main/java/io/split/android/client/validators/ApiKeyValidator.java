package io.split.android.client.validators;


/**
 * Interface to implement by ApiKey validators
 */
public interface ApiKeyValidator {

    /**
     * Checks that an SDK key is valid
     * @param sdkKey: SDK key string
     * @return true when the SDK key is valid, false when it is not
     */
    ValidationErrorInfo validate(String sdkKey);

}
