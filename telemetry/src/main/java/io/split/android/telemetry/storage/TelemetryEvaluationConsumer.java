package io.split.android.telemetry.storage;

import io.split.android.telemetry.model.MethodExceptions;
import io.split.android.telemetry.model.MethodLatencies;

public interface TelemetryEvaluationConsumer {

    MethodExceptions popExceptions();

    MethodLatencies popLatencies();
}
