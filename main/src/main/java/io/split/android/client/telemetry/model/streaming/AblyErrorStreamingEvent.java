package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class AblyErrorStreamingEvent extends StreamingEvent {

    public AblyErrorStreamingEvent(long errorCode, long timestamp) {
        super(EventTypeEnum.ABLY_ERROR, errorCode, timestamp);
    }
}
