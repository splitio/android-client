package io.split.android.client.submitter;

public interface RecorderTelemetry {
    void recordSuccess(long timestamp);
    void recordError(Integer httpStatus);
    void recordLatency(long latencyMs);
}
