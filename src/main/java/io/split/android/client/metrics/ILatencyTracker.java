package io.split.android.client.metrics;

/**
 * Created by patricioe on 2/10/16.
 */
interface ILatencyTracker {

    void addLatencyMillis(long millis);

    void addLatencyMicros(long micros);

    long[] getLatencies();

    long getLatency(int index);

    void clear();

    long getBucketForLatencyMillis(long latency);

    long getBucketForLatencyMicros(long latency);

}
