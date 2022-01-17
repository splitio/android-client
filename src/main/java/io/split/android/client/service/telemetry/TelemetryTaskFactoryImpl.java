package io.split.android.client.service.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;
import io.split.android.client.telemetry.storage.TelemetryConfigProviderImpl;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.telemetry.storage.TelemetryStatsProvider;
import io.split.android.client.telemetry.storage.TelemetryStatsProviderImpl;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageConsumer;

public class TelemetryTaskFactoryImpl implements TelemetryTaskFactory {

    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final HttpRecorder<Stats> mTelemetryStatsRecorder;
    private final TelemetryConfigProvider mTelemetryConfigProvider;
    private final TelemetryStatsProvider mTelemetryStatsProvider;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public TelemetryTaskFactoryImpl(@NonNull HttpRecorder<Config> telemetryConfigRecorder,
                                    @NonNull HttpRecorder<Stats> telemetryStatsRecorder,
                                    @NonNull TelemetryStorage telemetryStorage,
                                    @NonNull SplitClientConfig splitClientConfig,
                                    @NonNull SplitsStorage splitsStorage,
                                    @NonNull MySegmentsStorage mySegmentsStorage) {
        mTelemetryConfigRecorder = telemetryConfigRecorder;
        mTelemetryConfigProvider = new TelemetryConfigProviderImpl(telemetryStorage, splitClientConfig);
        mTelemetryStatsRecorder = telemetryStatsRecorder;
        mTelemetryStatsProvider = new TelemetryStatsProviderImpl(telemetryStorage, splitsStorage, mySegmentsStorage);
        mTelemetryRuntimeProducer = telemetryStorage;
    }

    @Override
    public TelemetryConfigRecorderTask getTelemetryConfigRecorderTask() {
        return new TelemetryConfigRecorderTask(mTelemetryConfigRecorder, mTelemetryConfigProvider, mTelemetryRuntimeProducer);
    }

    @Override
    public TelemetryStatsRecorderTask getTelemetryStatsRecorderTask() {
        return new TelemetryStatsRecorderTask(mTelemetryStatsRecorder, mTelemetryStatsProvider);
    }
}
