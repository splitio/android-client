package io.split.android.client.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetrySyncTaskExecutionListenerFactoryImpl implements TelemetrySyncTaskExecutionListenerFactory {

    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public TelemetrySyncTaskExecutionListenerFactoryImpl(@NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public TelemetrySyncTaskExecutionListener create(SplitTaskType taskType, OperationType operationType) {
        return new TelemetrySyncTaskExecutionListener(mTelemetryRuntimeProducer, taskType, operationType);
    }
}
