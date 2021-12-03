package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class OccupancyPriStreamingEvent extends StreamingEvent {

    public OccupancyPriStreamingEvent(long publishersCount, long timestamp) {
        super(EventTypeEnum.OCCUPANCY_PRI, publishersCount, timestamp);
    }
}
