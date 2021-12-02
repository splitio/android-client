package io.split.android.client.telemetry.storage.consumer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryConsumerImpl implements TelemetryConsumer {

    @NonNull
    private final TelemetryInitConsumer mTelemetryInitConsumer;

    @NonNull
    private final TelemetryRuntimeConsumer mTelemetryRuntimeConsumer;

    @NonNull
    private final TelemetryEvaluationConsumer mTelemetryEvaluationConsumer;

    public TelemetryConsumerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryInitConsumer = new TelemetryInitConsumerImpl(checkNotNull(telemetryStorage));
        mTelemetryRuntimeConsumer = new TelemetryRuntimeConsumerImpl(checkNotNull(telemetryStorage));
        mTelemetryEvaluationConsumer = new TelemetryEvaluationConsumerImpl(checkNotNull(telemetryStorage));
    }

    @NonNull
    @Override
    public TelemetryInitConsumer getTelemetryInitConsumer() {
        return mTelemetryInitConsumer;
    }

    @NonNull
    @Override
    public TelemetryRuntimeConsumer getTelemetryRuntimeConsumer() {
        return mTelemetryRuntimeConsumer;
    }

    @NonNull
    @Override
    public TelemetryEvaluationConsumer getTelemetryEvaluationConsumer() {
        return mTelemetryEvaluationConsumer;
    }
}
