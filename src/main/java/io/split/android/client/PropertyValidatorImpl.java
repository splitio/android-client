package io.split.android.client;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.PropertyValidator;
import io.split.android.client.validators.ValidationConfig;


public class PropertyValidatorImpl implements PropertyValidator {

    private final static int MAX_PROPS_COUNT = 300;
    private final static int MAXIMUM_EVENT_PROPERTY_BYTES =
            ValidationConfig.getInstance().getMaximumEventPropertyBytes();

    @Override
    public synchronized Result validate(Map<String, Object> properties, String validationTag) {
        if (properties == null) {
            return Result.valid(null, 0);
        }

        if (properties.size() > MAX_PROPS_COUNT) {
            Logger.w(validationTag + "Event has more than " + MAX_PROPS_COUNT +
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
                Logger.w(validationTag +
                        "The maximum size allowed for the " +
                        " properties is 32kb. Current is " + key +
                        ". Event not queued");
                return Result.invalid("Event properties size is too large", sizeInBytes);
            }
        }
        return Result.valid(finalProperties, sizeInBytes);
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
}
