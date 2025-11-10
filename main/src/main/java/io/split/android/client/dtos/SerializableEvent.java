package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;

public class SerializableEvent {

    public static final String EVENT_TYPE_FIELD = "eventTypeId";
    public static final String TRAFFIC_TYPE_NAME_FIELD = "trafficTypeName";
    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String PROPERTIES_FIELD = "properties";

    @SerializedName(EVENT_TYPE_FIELD)
    public String eventTypeId;
    @SerializedName(TRAFFIC_TYPE_NAME_FIELD)
    public String trafficTypeName;
    @SerializedName(KEY_FIELD)
    public String key;
    @SerializedName(VALUE_FIELD)
    public double value;
    @SerializedName(TIMESTAMP_FIELD)
    public long timestamp;
    @SerializedName(PROPERTIES_FIELD)
    public Map<String, Object> properties;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Double.compare(event.value, value) == 0 &&
                timestamp == event.timestamp &&
                Objects.equals(eventTypeId, event.eventTypeId) &&
                Objects.equals(trafficTypeName, event.trafficTypeName) &&
                Objects.equals(key, event.key) &&
                Objects.equals(properties, event.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventTypeId, trafficTypeName, key, value, timestamp);
    }
}
