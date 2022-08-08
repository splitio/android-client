package io.split.android.client.telemetry.model.streaming;

import com.google.gson.annotations.SerializedName;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class SseConnectionErrorStreamingEvent extends StreamingEvent {

    public SseConnectionErrorStreamingEvent(Status eventData, long timestamp) {
        super(EventTypeEnum.SSE_CONNECTION_ERROR, (long) eventData.getNumericValue(), timestamp);
    }

    public enum Status {
        @SerializedName("0")
        REQUESTED(0),
        @SerializedName("1")
        NON_REQUESTED(1);

        private final int numericValue;

        Status(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}
