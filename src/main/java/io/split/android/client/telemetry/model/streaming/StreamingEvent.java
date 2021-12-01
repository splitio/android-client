package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class StreamingEvent {

    private final EventTypeEnum eventType;

    private final Long eventData;

    private final long timestamp;

    public StreamingEvent(EventTypeEnum eventType, Long eventData, long timestamp) {
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public Long getEventData() {
        return eventData;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

