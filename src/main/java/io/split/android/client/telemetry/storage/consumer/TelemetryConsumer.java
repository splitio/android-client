package io.split.android.client.telemetry.storage.consumer;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryEvaluationConsumer;
import io.split.android.client.telemetry.storage.TelemetryInitConsumer;
import io.split.android.client.telemetry.storage.TelemetryRuntimeConsumer;

public interface TelemetryConsumer {

    @NonNull
    TelemetryInitConsumer getTelemetryInitConsumer();

    @NonNull
    TelemetryEvaluationConsumer getTelemetryEvaluationConsumer();

    @NonNull
    TelemetryRuntimeConsumer getTelemetryRuntimeConsumer();
}
