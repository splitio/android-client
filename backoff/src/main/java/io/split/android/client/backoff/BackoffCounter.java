package io.split.android.client.backoff;

public interface BackoffCounter {
    long getNextRetryTime();

    void resetCounter();
}
