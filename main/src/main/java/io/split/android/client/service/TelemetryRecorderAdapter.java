package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.submitter.RecorderTelemetry;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetryRecorderAdapter implements RecorderTelemetry {
    private final TelemetryRuntimeProducer mTelemetryProducer;
    private final OperationType mOperationType;

    public TelemetryRecorderAdapter(@NonNull TelemetryRuntimeProducer telemetryProducer,
                                    @NonNull OperationType operationType) {
        mTelemetryProducer = telemetryProducer;
        mOperationType = operationType;
    }

    @Override
    public void recordSuccess(long timestamp) {
        mTelemetryProducer.recordSuccessfulSync(mOperationType, timestamp);
    }

    @Override
    public void recordError(Integer httpStatus) {
        mTelemetryProducer.recordSyncError(mOperationType, httpStatus);
    }

    @Override
    public void recordLatency(long latencyMs) {
        mTelemetryProducer.recordSyncLatency(mOperationType, latencyMs);
    }
}
