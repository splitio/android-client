package io.split.android.client;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.ValidationConfig;


public class EventPropertiesProcessorImpl implements EventPropertiesProcessor {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L;
    private static final String VALIDATION_TAG = "track";
    private final static int MAX_PROPS_COUNT = 300;
    private final static int MAXIMUM_EVENT_PROPERTY_BYTES =
            ValidationConfig.getInstance().getMaximumEventPropertyBytes();


    private final TrackClientConfig mConfig;

    public EventPropertiesProcessorImpl(TrackClientConfig config) {
        mConfig = config;
    }

//    @Override
//    public boolean track(Event event) {
//
//        int sizeInBytes = EVENT_SIZE_WITHOUT_PROPS;
//        ProccessedEventProperties proccessedProperties = processProperties(event.properties);
//        if(proccessedProperties.isValid) {
//            return false;
//        }
//        event.setSizeInBytes(sizeInBytes);
//        return true;
//    }

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
            if (value == null) {
                continue;
            }

            String key = entry.getKey().toString();

            if (isInvalidValueType(value)) {
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
        return (value.getClass() == String.class ? value.toString().getBytes().length : 0) +
                key.getBytes().length;
    }


}
