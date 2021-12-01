package io.split.android.client.telemetry.storage;

public interface TelemetryInitConsumer {

    long getBURTimeouts();

    long getNonReadyUsage();
}
