package io.split.android.client.dtos;

import com.google.common.base.Objects;

import java.util.Map;

public class SerializableEvent {

    public String eventTypeId;
    public String trafficTypeName;
    public String key;
    public double value;
    public long timestamp;
    public Map<String, Object> properties;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Double.compare(event.value, value) == 0 &&
                timestamp == event.timestamp &&
                Objects.equal(eventTypeId, event.eventTypeId) &&
                Objects.equal(trafficTypeName, event.trafficTypeName) &&
                Objects.equal(key, event.key) &&
                Objects.equal(properties, event.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(eventTypeId, trafficTypeName, key, value, timestamp);
    }
}
