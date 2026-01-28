package io.split.android.client.telemetry.model.streaming;

import com.google.gson.annotations.SerializedName;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class StreamingEvent {

    @SerializedName("e")
    private final int eventType;

    @SerializedName("d")
    private final Long eventData;

    @SerializedName("t")
    private final long timestamp;

    public StreamingEvent(EventTypeEnum eventType, Long eventData, long timestamp) {
        this.eventType = eventType.getNumericValue();
        this.eventData = eventData;
        this.timestamp = timestamp;
    }

    public int getEventType() {
        return eventType;
    }

    public Long getEventData() {
        return eventData;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
