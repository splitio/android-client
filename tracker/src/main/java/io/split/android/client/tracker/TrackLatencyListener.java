package io.split.android.client.tracker;

/** Callback invoked with the track latency in milliseconds. May be null to skip telemetry. */
public interface TrackLatencyListener {
    void accept(long latencyMs);
}
