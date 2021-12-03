package io.split.android.client.telemetry.storage.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryRuntimeProducerImpl implements TelemetryRuntimeProducer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryRuntimeProducerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public void addTag(String tag) {
        mTelemetryStorage.addTag(tag);
    }

    @Override
    public void recordImpressionStats(ImpressionsDataType dataType, long count) {
        mTelemetryStorage.recordImpressionStats(dataType, count);
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        mTelemetryStorage.recordEventStats(dataType, count);
    }

    @Override
    public void recordSuccessfulSync(OperationType resource, long time) {
        mTelemetryStorage.recordSuccessfulSync(resource, time);
    }

    @Override
    public void recordSyncError(OperationType syncedResource, int status) {
        mTelemetryStorage.recordSyncError(syncedResource, status);
    }

    @Override
    public void recordSyncLatency(OperationType resource, long latency) {
        mTelemetryStorage.recordSyncLatency(resource, latency);
    }

    @Override
    public void recordAuthRejections() {
        mTelemetryStorage.recordAuthRejections();
    }

    @Override
    public void recordTokenRefreshes() {
        mTelemetryStorage.recordTokenRefreshes();
    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {
        mTelemetryStorage.recordStreamingEvents(streamingEvent);
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        mTelemetryStorage.recordSessionLength(sessionLength);
    }
}
