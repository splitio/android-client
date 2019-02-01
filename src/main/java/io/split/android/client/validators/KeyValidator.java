package io.split.android.client.validators;

import io.split.android.client.api.Key;

/**
 * Interface to implement by Key validators
 */
public interface KeyValidator {

    /**
     * Checks that a Key is valid
     * @param key: Key instance
     * @return true when the key is valid, false when it is not
     */
    public boolean isValidKey(Key key);

    /**
     * Checks that a Key is valid
     * @param matchingKey: Matching key
     * @param bucketingKey: Bucketing key
     * @return true when the key is valid, false when it is not
     */
    public boolean isValidKey(String matchingKey, String bucketingKey);

    /**
     * Overrides de default message logger
     * @param logger: An implementation of ValidationMessageLogger
     *
     */
    public void setMessageLogger(ValidationMessageLogger logger);

    /**
     * Sets the tag displayed in logs
     * @param tag: String tag
     *
     */
    public void setTag(String tag);
}
