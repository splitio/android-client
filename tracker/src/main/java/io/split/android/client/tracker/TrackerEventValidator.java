package io.split.android.client.tracker;

import java.util.Map;

/**
 * Validates event parameters before tracking.
 * Returns null if valid, or a {@link TrackerValidationError} with error/warning info.
 */
public interface TrackerEventValidator {
    TrackerValidationError validate(String key, String trafficTypeName, String eventTypeId,
                                    Double value, Map<String, Object> properties, boolean isSdkReady);
}
