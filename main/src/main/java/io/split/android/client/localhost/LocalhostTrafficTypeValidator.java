package io.split.android.client.localhost;

import io.split.android.client.tracker.TrafficTypeValidator;

/**
 * Traffic type validator for localhost mode.
 * <p>
 * In localhost mode, all traffic types are considered valid since we're not
 * connected to the Split backend and can't validate against real feature flags.
 */
public class LocalhostTrafficTypeValidator implements TrafficTypeValidator {

    @Override
    public boolean isValid(String trafficTypeName) {
        // In localhost mode, accept all traffic types
        return true;
    }
}
