package io.split.android.client.telemetry.storage.consumer;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryEvaluationConsumer;
import io.split.android.client.telemetry.storage.TelemetryInitConsumer;
import io.split.android.client.telemetry.storage.TelemetryRuntimeConsumer;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryConsumerImpl implements TelemetryConsumer {

    public TelemetryConsumerImpl(@NonNull TelemetryStorage telemetryStorage) {
    }

        @NonNull
    @Override
    public TelemetryInitConsumer getTelemetryInitConsumer() {
        return null;
    }

    @NonNull
    @Override
    public TelemetryEvaluationConsumer getTelemetryEvaluationConsumer() {
        return null;
    }

    @NonNull
    @Override
    public TelemetryRuntimeConsumer getTelemetryRuntimeConsumer() {
        return null;
    }
}
