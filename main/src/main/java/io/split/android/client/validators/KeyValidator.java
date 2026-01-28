package io.split.android.client.validators;

/**
 * Interface to implement by Key validators
 */
public interface KeyValidator {

    /**
     * Checks that a Key is valid
     * @param matchingKey: Matching key
     * @param bucketingKey: Bucketing key
     * @return true when the key is valid, false when it is not
     */
    ValidationErrorInfo validate(String matchingKey, String bucketingKey);
}
