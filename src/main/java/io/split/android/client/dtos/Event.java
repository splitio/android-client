package io.split.android.client.dtos;

import com.google.common.base.Objects;

import java.util.Map;

import io.split.android.client.storage.InBytesSizable;

public class Event implements InBytesSizable, Identifiable {

    public String eventTypeId;
    public String trafficTypeName;
    public String key;
    public double value;
    public long timestamp;
    public Map<String,Object> properties;

    transient public long storageId;
    private int sizeInBytes = 0;

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

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public long getId() {
        return storageId;
    }
}
