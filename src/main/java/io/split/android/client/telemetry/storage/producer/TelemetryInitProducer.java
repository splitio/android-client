package io.split.android.client.telemetry.storage.producer;

public interface TelemetryInitProducer {

    void recordBURTimeout();

    void recordNonReadyUsage();
}
