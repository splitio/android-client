package io.split.android.client.telemetry.storage;

import io.split.android.client.telemetry.model.Config;

/**
 * Builds the {@link io.split.android.client.telemetry.model.Config} object to be sent.
 */
public interface TelemetryConfigProvider {

    Config getConfigTelemetry();
}
