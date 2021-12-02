package io.split.android.client.telemetry.storage.consumer;

import androidx.annotation.NonNull;

public interface TelemetryConsumer {

    @NonNull
    TelemetryInitConsumer getTelemetryInitConsumer();

    @NonNull
    TelemetryEvaluationConsumer getTelemetryEvaluationConsumer();

    @NonNull
    TelemetryRuntimeConsumer getTelemetryRuntimeConsumer();
}
