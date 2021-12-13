package io.split.android.client.service.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;
import io.split.android.client.telemetry.storage.TelemetryConfigProviderImpl;
import io.split.android.client.telemetry.storage.TelemetryStorageConsumer;

public class TelemetryTaskFactoryImpl implements TelemetryTaskFactory {

    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final TelemetryConfigProvider mTelemetryConfigProvider;

    public TelemetryTaskFactoryImpl(@NonNull HttpRecorder<Config> telemetryConfigRecorder,
                                    @NonNull TelemetryStorageConsumer telemetryConsumer,
                                    @NonNull SplitClientConfig splitClientConfig) {
        mTelemetryConfigRecorder = telemetryConfigRecorder;
        mTelemetryConfigProvider = new TelemetryConfigProviderImpl(telemetryConsumer, splitClientConfig);
    }

    @Override
    public TelemetryConfigRecorderTask getTelemetryConfigRecorderTask() {
        return new TelemetryConfigRecorderTask(mTelemetryConfigRecorder, mTelemetryConfigProvider);
    }
}
