package io.split.android.client.telemetry.storage;

import io.split.android.client.telemetry.model.Stats;

public interface TelemetryStatsProvider {

    Stats getTelemetryStats();

    void clearStats();
}
