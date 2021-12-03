package io.split.android.client.telemetry.storage.consumer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryRuntimeConsumerImpl implements TelemetryRuntimeConsumer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryRuntimeConsumerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public long getImpressionsStats(ImpressionsDataType type) {
        return mTelemetryStorage.getImpressionsStats(type);
    }

    @Override
    public long getEventsStats(EventsDataRecordsEnum type) {
        return mTelemetryStorage.getEventsStats(type);
    }

    @Override
    public LastSync getLastSynchronization() {
        return mTelemetryStorage.getLastSynchronization();
    }

    @Override
    public HttpErrors popHttpErrors() {
        return mTelemetryStorage.popHttpErrors();
    }

    @Override
    public HttpLatencies popHttpLatencies() {
        return mTelemetryStorage.popHttpLatencies();
    }

    @Override
    public long popAuthRejections() {
        return mTelemetryStorage.popAuthRejections();
    }

    @Override
    public long popTokenRefreshes() {
        return mTelemetryStorage.popTokenRefreshes();
    }

    @Override
    public List<StreamingEvent> popStreamingEvents() {
        return mTelemetryStorage.popStreamingEvents();
    }

    @Override
    public List<String> popTags() {
        return mTelemetryStorage.popTags();
    }

    @Override
    public long getSessionLength() {
        return mTelemetryStorage.getSessionLength();
    }
}
