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
    public boolean isValidApiKey(String apiKey);

    /**
     * Overrides de default message logger
     * @param logger: An implementation of ValidationMessageLogger
     *
     */
    public void setMessageLogger(ValidationMessageLogger logger);

}
