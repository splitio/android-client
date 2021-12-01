package io.split.android.client.telemetry.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.util.AtomicLongArray;

public class TelemetryStorageImpl implements TelemetryEvaluationProducer, TelemetryEvaluationConsumer {

    private static final int MAX_LATENCY_BUCKET_COUNT = 23;
    private static final int MAX_STREAMING_EVENTS = 20;
    private static final int MAX_TAGS = 10;

    private final Map<Method, AtomicLong> methodExceptionsCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<Method, AtomicLongArray> methodLatencies = new ConcurrentHashMap<>();

    public TelemetryStorageImpl() {
        initializeMethodExceptionsCounter();
        initializeHttpLatenciesCounter();
    }

    private void initializeHttpLatenciesCounter() {
        methodLatencies.put(Method.TREATMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENT_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENTS_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TRACK, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
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
        methodExceptions.setTreatments(methodExceptionsCounter.get(Method.TREATMENTS).getAndSet(0L));
        methodExceptions.setTreatmentWithConfig(methodExceptionsCounter.get(Method.TREATMENT_WITH_CONFIG).getAndSet(0L));
        methodExceptions.setTreatmentsWithConfig(methodExceptionsCounter.get(Method.TREATMENTS_WITH_CONFIG).getAndSet(0L));
        methodExceptions.setTrack(methodExceptionsCounter.get(Method.TRACK).getAndSet(0L));

        return methodExceptions;
    }

    @Override
    public MethodLatencies popLatencies() {
        MethodLatencies latencies = new MethodLatencies();

        latencies.setTreatment(methodLatencies.get(Method.TREATMENT).fetchAndClearAll());
        latencies.setTreatments(methodLatencies.get(Method.TREATMENTS).fetchAndClearAll());
        latencies.setTreatmentWithConfig(methodLatencies.get(Method.TREATMENT_WITH_CONFIG).fetchAndClearAll());
        latencies.setTreatmentsWithConfig(methodLatencies.get(Method.TREATMENTS_WITH_CONFIG).fetchAndClearAll());
        latencies.setTrack(methodLatencies.get(Method.TRACK).fetchAndClearAll());

        return latencies;
    }

    @Override
    public void recordLatency(Method method, long latency) {
        BinarySearchLatencyTracker binarySearchLatencyTracker = new BinarySearchLatencyTracker();
        long bucketForLatencyMillis = binarySearchLatencyTracker.getBucketForLatencyMillis(latency);

        methodLatencies.get(method).increment((int) bucketForLatencyMillis);
    }

    @Override
    public void recordException(Method method) {
        methodExceptionsCounter.get(method).incrementAndGet();
    }
}
