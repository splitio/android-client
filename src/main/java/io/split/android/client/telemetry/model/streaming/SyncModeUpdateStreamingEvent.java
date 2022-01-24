package io.split.android.client.telemetry.model.streaming;

import com.google.gson.annotations.SerializedName;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class SyncModeUpdateStreamingEvent extends StreamingEvent {

    public SyncModeUpdateStreamingEvent(Mode mode, long timestamp) {
        super(EventTypeEnum.SYNC_MODE_UPDATE, (long) mode.getNumericValue(), timestamp);
    }

    public enum Mode {
        @SerializedName("0")
        STREAMING(0),
        @SerializedName("1")
        POLLING(1);

        private final int numericValue;

        Mode(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}
