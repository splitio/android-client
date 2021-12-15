package io.split.android.client.telemetry.storage;

public interface TelemetryInitConsumer {

    long getNonReadyUsage();

    long getTimeUntilReadyFromCache();
}
