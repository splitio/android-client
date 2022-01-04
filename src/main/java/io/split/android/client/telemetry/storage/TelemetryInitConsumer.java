package io.split.android.client.telemetry.storage;

public interface TelemetryInitConsumer {

    long getNonReadyUsage();

    long getActiveFactories();

    long getRedundantFactories();

    long getTimeUntilReady();

    long getTimeUntilReadyFromCache();
}
