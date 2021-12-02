package io.split.android.client.telemetry.storage.consumer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryInitConsumerImpl implements TelemetryInitConsumer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryInitConsumerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public long getBURTimeouts() {
        return mTelemetryStorage.getBURTimeouts();
    }

    @Override
    public long getNonReadyUsage() {
        return mTelemetryStorage.getNonReadyUsage();
    }
}
