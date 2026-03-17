package io.split.android.client.validators;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.tracker.TrackerLogger;
import io.split.android.client.tracker.TrackerPropertyValidator;


public class PropertyValidatorImpl implements TrackerPropertyValidator {

    private final TrackerLogger mLogger;

    private final static int MAX_PROPS_COUNT = 300;
    private final static int MAXIMUM_EVENT_PROPERTY_BYTES =
            ValidationConfig.getInstance().getMaximumEventPropertyBytes();

    public PropertyValidatorImpl(TrackerLogger logger) {
        mLogger = logger;
    }

    /**
     * Internal validation logic - returns a simple result with properties and size.
     */
    private InternalResult validateInternal(Map<String, Object> properties, String validationTag) {
        if (properties == null) {
            return new InternalResult(true, null, 0, null);
        }

        if (properties.size() > MAX_PROPS_COUNT) {
            mLogger.v(validationTag + "Event has more than " + MAX_PROPS_COUNT +
                    " properties. Some of them will be trimmed when processed");
        }
        int sizeInBytes = 0;
        Map<String, Object> finalProperties = new HashMap<>(properties);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value != null && isInvalidValueType(value)) {
                finalProperties.put(key, null);
            }
            sizeInBytes += calculateEventSizeInBytes(key, value);

            if (sizeInBytes > MAXIMUM_EVENT_PROPERTY_BYTES) {
                mLogger.v(validationTag +
                        "The maximum size allowed for the " +
                        " properties is 32kb. Current is " + key +
                        ". Event not queued");
                return new InternalResult(false, null, sizeInBytes, "Event properties size is too large");
            }
        }
        return new InternalResult(true, finalProperties, sizeInBytes, null);
    }

    private static boolean isInvalidValueType(Object value) {
        return !(value instanceof Number) &&
                !(value instanceof Boolean) &&
                !(value instanceof String);
    }

    private static int calculateEventSizeInBytes(String key, Object value) {
        int valueSize = 0;
        if(value != null && value.getClass() == String.class) {
            valueSize = value.toString().getBytes().length;
        }
        return valueSize + key.getBytes().length;
    }

    @Override
    public TrackerPropertyResult validate(Map<String, Object> properties, int initialSizeInBytes,
                                          String validationTag) {
        InternalResult result = validateInternal(properties, validationTag);
        int totalSize = initialSizeInBytes + result.sizeInBytes;
        if (result.isValid) {
            return TrackerPropertyResult.valid(result.properties, totalSize);
        } else {
            return TrackerPropertyResult.invalid(result.errorMessage, totalSize);
        }
    }

    /**
     * Internal result class to avoid depending on main module's PropertyValidator.Result.
     */
    private static class InternalResult {
        final boolean isValid;
        final Map<String, Object> properties;
        final int sizeInBytes;
        final String errorMessage;

        InternalResult(boolean isValid, Map<String, Object> properties, int sizeInBytes, String errorMessage) {
            this.isValid = isValid;
            this.properties = properties;
            this.sizeInBytes = sizeInBytes;
            this.errorMessage = errorMessage;
        }
    }
}
