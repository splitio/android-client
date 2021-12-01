package io.split.android.client.telemetry.storage;

public interface TelemetryInitProducer {

    void recordBURTimeout();

    void recordNonReadyUsage();
}
