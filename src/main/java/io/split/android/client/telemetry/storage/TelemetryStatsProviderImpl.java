package io.split.android.client.telemetry.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.model.Stats;

public class TelemetryStatsProviderImpl implements TelemetryStatsProvider {

    private TelemetryStorageConsumer mTelemetryStorageConsumer;

    public TelemetryStatsProviderImpl(@NonNull TelemetryStorageConsumer telemetryStorageConsumer) {
        mTelemetryStorageConsumer = checkNotNull(telemetryStorageConsumer);
    }

    @Override
    public Stats getTelemetryStats() {
        return new Stats();
    }
}
