package io.split.android.client.service.telemetry;

public interface TelemetryTaskFactory {

    TelemetryConfigRecorderTask getTelemetryConfigRecorderTask();

    TelemetryStatsRecorderTask getTelemetryStatsRecorderTask();
}
