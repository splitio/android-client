package io.split.android.client.service.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;
import io.split.android.client.telemetry.storage.TelemetryConfigProviderImpl;
import io.split.android.client.telemetry.storage.TelemetryStatsProvider;
import io.split.android.client.telemetry.storage.TelemetryStatsProviderImpl;
import io.split.android.client.telemetry.storage.TelemetryStorageConsumer;

public class TelemetryTaskFactoryImpl implements TelemetryTaskFactory {

    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final HttpRecorder<Stats> mTelemetryStatsRecorder;
    private final TelemetryConfigProvider mTelemetryConfigProvider;
    private final TelemetryStatsProvider mTelemetryStatsProvider;

    public TelemetryTaskFactoryImpl(@NonNull HttpRecorder<Config> telemetryConfigRecorder,
                                    @NonNull HttpRecorder<Stats> telemetryStatsRecorder,
                                    @NonNull TelemetryStorageConsumer telemetryConsumer,
                                    @NonNull SplitClientConfig splitClientConfig) {
        mTelemetryConfigRecorder = telemetryConfigRecorder;
        mTelemetryConfigProvider = new TelemetryConfigProviderImpl(telemetryConsumer, splitClientConfig);
        mTelemetryStatsRecorder = telemetryStatsRecorder;
        mTelemetryStatsProvider = new TelemetryStatsProviderImpl(telemetryConsumer);
    }

    @Override
    public TelemetryConfigRecorderTask getTelemetryConfigRecorderTask() {
        return new TelemetryConfigRecorderTask(mTelemetryConfigRecorder, mTelemetryConfigProvider);
    }

    @Override
    public TelemetryStatsRecorderTask getTelemetryStatsRecorderTask() {
        return new TelemetryStatsRecorderTask(mTelemetryStatsRecorder, mTelemetryStatsProvider);
    }
}
