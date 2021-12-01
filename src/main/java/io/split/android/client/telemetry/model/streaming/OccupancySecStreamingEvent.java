package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class OccupancySecStreamingEvent extends StreamingEvent {

    public OccupancySecStreamingEvent(long publishersCount, long timestamp) {
        super(EventTypeEnum.OCCUPANCY_SEC, publishersCount, timestamp);
    }
}
