package io.split.android.client.tracker;

/**
 * Interface for validating traffic type names.
 * <p>
 * This abstraction allows different implementations:
 * - Production: delegates to SplitsStorage to check if traffic type exists
 * - Localhost: always returns true (any traffic type is valid)
 * - Testing: fully mockable
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
