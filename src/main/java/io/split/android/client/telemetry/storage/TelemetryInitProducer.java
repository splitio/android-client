package io.split.android.client.telemetry.storage;

public interface TelemetryInitProducer {

    void recordNonReadyUsage();

    void recordActiveFactories(int count);

    void recordRedundantFactories(int count);

    void recordTimeUntilReady(long time);

    void recordTimeUntilReadyFromCache(long timeUntilReadyFromCache);
}
