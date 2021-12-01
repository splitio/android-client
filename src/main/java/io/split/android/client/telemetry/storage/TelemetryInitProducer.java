package io.split.android.client.telemetry.storage;

import io.split.android.client.telemetry.model.Config;

public interface TelemetryInitProducer {

    void recordConfig(Config config);

    void recordBURTimeout();

    void recordNonReadyUsage();
}
