package io.split.android.client.telemetry.storage;

import io.split.android.client.telemetry.model.Method;

public interface TelemetryEvaluationProducer {

    void recordLatency(Method method, long latency);

    void recordException(Method method);
}
