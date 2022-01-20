package io.split.android.client.telemetry.model.streaming;

import com.google.gson.annotations.SerializedName;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class StreamingStatusStreamingEvent extends StreamingEvent {

    public StreamingStatusStreamingEvent(Status eventData, long timestamp) {
        super(EventTypeEnum.STREAMING_STATUS, (long) eventData.getNumericValue(), timestamp);
    }

    public enum Status {
        @SerializedName("0")
        DISABLED(0),
        @SerializedName("1")
        ENABLED(1),
        @SerializedName("2")
        PAUSED(2);

        private final int numericValue;

        Status(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}
