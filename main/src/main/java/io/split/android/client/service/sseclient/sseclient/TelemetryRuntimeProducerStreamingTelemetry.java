package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import io.split.android.client.service.sseclient.spi.StreamingTelemetry;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.AblyErrorStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancyPriStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;
import io.split.android.client.telemetry.model.streaming.SseConnectionErrorStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

/**
 * Adapter that implements StreamingTelemetry using TelemetryRuntimeProducer.
 */
public class TelemetryRuntimeProducerStreamingTelemetry implements StreamingTelemetry {

    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public TelemetryRuntimeProducerStreamingTelemetry(@NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryRuntimeProducer = telemetryRuntimeProducer;
    }

    @Override
    public void recordTokenSyncLatency(long latencyMillis) {
        mTelemetryRuntimeProducer.recordSyncLatency(OperationType.TOKEN, latencyMillis);
    }

    @Override
    public void recordTokenSuccessfulSync(long timestamp) {
        mTelemetryRuntimeProducer.recordSuccessfulSync(OperationType.TOKEN, timestamp);
    }

    @Override
    public void recordTokenSyncError(Integer httpStatus) {
        mTelemetryRuntimeProducer.recordSyncError(OperationType.TOKEN, httpStatus);
    }

    @Override
    public void recordAuthRejections() {
        mTelemetryRuntimeProducer.recordAuthRejections();
    }

    @Override
    public void recordTokenRefreshes() {
        mTelemetryRuntimeProducer.recordTokenRefreshes();
    }

    @Override
    public void recordTokenRefreshEvent(long expirationTime, long timestamp) {
        mTelemetryRuntimeProducer.recordStreamingEvents(new TokenRefreshStreamingEvent(expirationTime, timestamp));
    }

    @Override
    public void recordSyncModeUpdate(boolean streaming, long timestamp) {
        SyncModeUpdateStreamingEvent.Mode mode = streaming
                ? SyncModeUpdateStreamingEvent.Mode.STREAMING
                : SyncModeUpdateStreamingEvent.Mode.POLLING;
        mTelemetryRuntimeProducer.recordStreamingEvents(new SyncModeUpdateStreamingEvent(mode, timestamp));
    }

    @Override
    public void recordConnectionError(boolean retryable, long timestamp) {
        SseConnectionErrorStreamingEvent.Status status = retryable
                ? SseConnectionErrorStreamingEvent.Status.REQUESTED
                : SseConnectionErrorStreamingEvent.Status.NON_REQUESTED;
        mTelemetryRuntimeProducer.recordStreamingEvents(new SseConnectionErrorStreamingEvent(status, timestamp));
    }

    @Override
    public void recordAblyError(int errorCode, long timestamp) {
        mTelemetryRuntimeProducer.recordStreamingEvents(new AblyErrorStreamingEvent(errorCode, timestamp));
    }

    @Override
    public void recordOccupancyPri(int publisherCount, long timestamp) {
        mTelemetryRuntimeProducer.recordStreamingEvents(new OccupancyPriStreamingEvent(publisherCount, timestamp));
    }

    @Override
    public void recordOccupancySec(int publisherCount, long timestamp) {
        mTelemetryRuntimeProducer.recordStreamingEvents(new OccupancySecStreamingEvent(publisherCount, timestamp));
    }

    @Override
    public void recordStreamingStatus(StreamingStatus status, long timestamp) {
        StreamingStatusStreamingEvent.Status telemetryStatus;
        switch (status) {
            case PAUSED:
                telemetryStatus = StreamingStatusStreamingEvent.Status.PAUSED;
                break;
            case DISABLED:
                telemetryStatus = StreamingStatusStreamingEvent.Status.DISABLED;
                break;
            case ENABLED:
            default:
                telemetryStatus = StreamingStatusStreamingEvent.Status.ENABLED;
                break;
        }
        mTelemetryRuntimeProducer.recordStreamingEvents(new StreamingStatusStreamingEvent(telemetryStatus, timestamp));
    }
}
