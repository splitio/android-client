package io.split.android.client.telemetry.storage;

public interface TelemetryInitProducer {

    void recordNonReadyUsage();

    void recordTimeUntilReadyFromCache(long timeUntilReadyFromCache);
}
