package io.split.android.client.service.sseclient;

public interface BackoffCounter {
    long getNextRetryTime();

    void resetCounter();
}
