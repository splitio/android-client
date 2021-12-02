package io.split.android.client.telemetry.storage.consumer;

public interface TelemetryInitConsumer {

    long getBURTimeouts();

    long getNonReadyUsage();
}
