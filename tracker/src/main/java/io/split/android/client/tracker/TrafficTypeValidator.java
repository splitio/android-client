package io.split.android.client.tracker;

/**
 * Interface for validating traffic type names.
 */
public interface TrafficTypeValidator {
    /**
     * Checks if the given traffic type name is valid.
     *
     * @param trafficTypeName the traffic type name to validate
     * @return true if the traffic type is valid, false otherwise
     */
    boolean isValid(String trafficTypeName);
}
