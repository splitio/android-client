package io.split.android.telemetry.storage;

import io.split.android.telemetry.model.Method;

public interface TelemetryEvaluationProducer {

    void recordLatency(Method method, long latency);

    void recordException(Method method);
}
