package io.split.android.telemetry.storage;

import io.split.android.telemetry.model.Config;

public interface TelemetryInitProducer {

    void recordConfig(Config config);

    void recordBURTimeout();

    void recordNonReadyUsage();
}
