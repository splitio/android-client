package io.split.android.client.telemetry.storage.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryInitProducerImpl implements TelemetryInitProducer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryInitProducerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public void recordBURTimeout() {
        mTelemetryStorage.recordBURTimeout();
    }

    @Override
    public void recordNonReadyUsage() {
        mTelemetryStorage.recordNonReadyUsage();
    }
}
