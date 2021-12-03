package io.split.android.client.telemetry.storage.producer;

import androidx.annotation.NonNull;

public class TelemetryProducerImpl implements TelemetryProducer {

    private final TelemetryInitProducer mTelemetryInitProducer;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final TelemetryEvaluationProducer mTelemetryEvaluationProducer;

    public TelemetryProducerImpl(@NonNull TelemetryInitProducer telemetryInitProducer,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull TelemetryEvaluationProducer telemetryEvaluationProducer) {
        this.mTelemetryInitProducer = telemetryInitProducer;
        this.mTelemetryRuntimeProducer = telemetryRuntimeProducer;
        this.mTelemetryEvaluationProducer = telemetryEvaluationProducer;
    }

    @Override
    public TelemetryInitProducer getTelemetryInitProducer() {
        return mTelemetryInitProducer;
    }

    @Override
    public TelemetryRuntimeProducer getTelemetryRuntimeProducer() {
        return mTelemetryRuntimeProducer;
    }

    @Override
    public TelemetryEvaluationProducer getTelemetryEvaluationProducer() {
        return mTelemetryEvaluationProducer;
    }
}
