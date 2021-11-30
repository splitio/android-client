package io.split.android.telemetry.model;

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

class ConnectionEstablished extends StreamingEvent {

    public ConnectionEstablished(long timestamp) {
        super(EventTypeEnum.CONNECTION_ESTABLISHED, null, timestamp);
    }
}

class OccupancyPri extends StreamingEvent {

    public OccupancyPri(long publishersCount, long timestamp) {
        super(EventTypeEnum.OCCUPANCY_PRI, publishersCount, timestamp);
    }
}

class OccupancySec extends StreamingEvent {

    public OccupancySec(long publishersCount, long timestamp) {
        super(EventTypeEnum.OCCUPANCY_SEC, publishersCount, timestamp);
    }
}

class StreamingStatus extends StreamingEvent {

    public StreamingStatus(Status eventData, long timestamp) {
        super(EventTypeEnum.STREAMING_STATUS, (long) eventData.getNumericValue(), timestamp);
    }

    enum Status {
        DISABLED(0), ENABLED(1), PAUSED(2);

        private final int numericValue;

        Status(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}

class SseConnectionError extends StreamingEvent {

    public SseConnectionError(Status eventData, long timestamp) {
        super(EventTypeEnum.SSE_CONNECTION_ERROR, (long) eventData.getNumericValue(), timestamp);
    }

    enum Status {
        REQUESTED(0), NON_REQUESTED(1);

        private final int numericValue;

        Status(int numericValue) {
            this.numericValue = numericValue;
        }

        public int getNumericValue() {
            return numericValue;
        }
    }
}

class TokenRefresh extends StreamingEvent {

    public TokenRefresh(long tokenExpirationUTC, long timestamp) {
        super(EventTypeEnum.TOKEN_REFRESH, tokenExpirationUTC, timestamp);
    }
}

class AblyError extends StreamingEvent {

    public AblyError(long errorCode, long timestamp) {
        super(EventTypeEnum.ABLY_ERROR, errorCode, timestamp);
    }
}

class SyncModeUpdate extends StreamingEvent {

    public SyncModeUpdate(Mode mode, long timestamp) {
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
