package io.split.android.client.telemetry.storage.consumer;

import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;

public interface TelemetryEvaluationConsumer {

    MethodExceptions popExceptions();

    MethodLatencies popLatencies();
}
