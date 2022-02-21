package io.split.android.client.dtos;

import java.util.Map;

/**
 * Represents the model expected by the API.
 */
public class SerializedEvent {
    public final String eventTypeId;
    public final String trafficTypeName;
    public final String key;
    public final double value;
    public final long timestamp;
    public final Map<String, Object> properties;

    public SerializedEvent(Event event) {
        eventTypeId = event.eventTypeId;
        trafficTypeName = event.trafficTypeName;
        key = event.key;
        value = event.value;
        timestamp = event.timestamp;
        properties = event.properties;
    }
}
