package io.split.android.client.service.sseclient;

public class FixedIntervalBackoffCounter implements BackoffCounter {

    private final long mRetryInterval;

    /**
     * @param retryInterval Interval for retries in seconds.
     */
    public FixedIntervalBackoffCounter(long retryInterval) {
        mRetryInterval = retryInterval;
    }

    @Override
    public long getNextRetryTime() {
        return mRetryInterval;
    }

    @Override
    public void resetCounter() {
        // no-op
    }
}
