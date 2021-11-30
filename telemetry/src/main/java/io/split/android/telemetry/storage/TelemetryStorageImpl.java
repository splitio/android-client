package io.split.android.telemetry.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.telemetry.model.Method;
import io.split.android.telemetry.model.MethodExceptions;
import io.split.android.telemetry.model.MethodLatencies;

public class TelemetryStorageImpl implements TelemetryEvaluationProducer, TelemetryEvaluationConsumer {

    private final Map<Method, AtomicLong> methodExceptionsCounter = new ConcurrentHashMap<>();

    public TelemetryStorageImpl() {
        initializeMethodExceptionsCounter();
    }

    private void initializeMethodExceptionsCounter() {
        methodExceptionsCounter.put(Method.TREATMENT, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENT_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TRACK, new AtomicLong());
    }

    @Override
    public MethodExceptions popExceptions() {
        MethodExceptions methodExceptions = new MethodExceptions();

        methodExceptions.setTreatment(methodExceptionsCounter.get(Method.TREATMENT).getAndSet(0L));

        return methodExceptions;
    }

    @Override
    public MethodLatencies popLatencies() {

    }

    @Override
    public void recordLatency(Method method, long latency) {

    }

    @Override
    public void recordException(Method method) {

    }
}
