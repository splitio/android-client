package io.split.android.client.validators;


/**
 * Interface to implement by ApiKey validators
 */
public interface ApiKeyValidator {

    /**
     * Checks that an ApiKey is valid
     * @param apiKey: ApiKey string
     * @return true when the api key is valid, false when it is not
     */
    ValidationErrorInfo validate(String apiKey);

}
