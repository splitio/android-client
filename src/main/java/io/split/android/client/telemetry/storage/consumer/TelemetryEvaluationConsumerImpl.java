package io.split.android.client.telemetry.storage.consumer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryEvaluationConsumerImpl implements TelemetryEvaluationConsumer {

    private final TelemetryStorage mTelemetryStorage;

    public TelemetryEvaluationConsumerImpl(@NonNull TelemetryStorage telemetryStorage) {
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    @Override
    public MethodExceptions popExceptions() {
        return mTelemetryStorage.popExceptions();
    }

    @Override
    public MethodLatencies popLatencies() {
        return mTelemetryStorage.popLatencies();
    }
}
