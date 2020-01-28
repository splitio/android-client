package io.split.android.client;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.Logger;
import io.split.android.client.validators.ValidationConfig;


public class EventPropertiesProcessorImpl implements EventPropertiesProcessor {

    private static final String VALIDATION_TAG = "track";
    private final static int MAX_PROPS_COUNT = 300;
    private final static int MAXIMUM_EVENT_PROPERTY_BYTES =
            ValidationConfig.getInstance().getMaximumEventPropertyBytes();

    @Override
    public ProcessedEventProperties process(Map<String, Object> properties) {
        if (properties == null) {
            return new ProcessedEventProperties(true, null, 0);
        }

        if (properties.size() > MAX_PROPS_COUNT) {
            Logger.w(VALIDATION_TAG + "Event has more than " + MAX_PROPS_COUNT +
                    " properties. Some of them will be trimmed when processed");
        }
        int sizeInBytes = 0;
        Map<String, Object> finalProperties = new HashMap<>(properties);

        for (Map.Entry entry : properties.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey().toString();

            if (value != null && isInvalidValueType(value)) {
                finalProperties.put(key, null);
            }
            sizeInBytes += calculateEventSizeInBytes(key, value);

            if (sizeInBytes > MAXIMUM_EVENT_PROPERTY_BYTES) {
                Logger.w(VALIDATION_TAG +
                        "The maximum size allowed for the " +
                        " properties is 32kb. Current is " + key +
                        ". Event not queued");
                return ProcessedEventProperties.InvalidProperties();
            }
        }
        return new ProcessedEventProperties(true, finalProperties, sizeInBytes);
    }

    private boolean isInvalidValueType(Object value) {
        return !(value instanceof Number) &&
                !(value instanceof Boolean) &&
                !(value instanceof String);
    }

    private int calculateEventSizeInBytes(String key, Object value) {
        int valueSize = 0;
        if(value != null && value.getClass() == String.class) {
            valueSize = value.toString().getBytes().length;
        }
        return valueSize + key.getBytes().length;
    }


}
