package io.split.android.client.telemetry.storage.producer;

public interface TelemetryProducer {

    TelemetryInitProducer getTelemetryInitProducer();

    TelemetryRuntimeProducer getTelemetryRuntimeProducer();

    TelemetryEvaluationProducer getTelemetryEvaluationProducer();
}
