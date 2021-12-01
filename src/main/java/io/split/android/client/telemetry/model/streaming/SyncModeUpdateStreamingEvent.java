package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class SyncModeUpdateStreamingEvent extends StreamingEvent {

    public SyncModeUpdateStreamingEvent(Mode mode, long timestamp) {
        super(EventTypeEnum.SYNC_MODE_UPDATE, (long) mode.getNumericValue(), timestamp);
    }

    enum Mode {
        STREAMING(0), POLLING(1);

        private final int numericValue;

        Mode(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}
