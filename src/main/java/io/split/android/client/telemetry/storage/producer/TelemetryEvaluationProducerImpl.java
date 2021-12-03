package io.split.android.client.telemetry.storage.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryEvaluationProducerImpl implements TelemetryEvaluationProducer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryEvaluationProducerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public void recordLatency(Method method, long latency) {
        mTelemetryStorage.recordLatency(method, latency);
    }

    @Override
    public void recordException(Method method) {
        mTelemetryStorage.recordException(method);
    }
}
