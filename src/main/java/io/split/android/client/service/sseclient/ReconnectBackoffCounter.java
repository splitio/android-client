package io.split.android.client.service.sseclient;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectBackoffCounter implements BackoffCounter {
    private final static int MAX_TIME_LIMIT_IN_SECS = 1800; // 30 minutes (30 * 60)
    private final static int RETRY_EXPONENTIAL_BASE = 2;
    private final int mBackoffBase;
    private final AtomicLong mAttemptCount;

    public ReconnectBackoffCounter(int mBackoffBase) {
        this.mBackoffBase = mBackoffBase;
        mAttemptCount = new AtomicLong(0);
    }

    @Override
    public long getNextRetryTime() {
        long retryTime = (long) Math.pow(mBackoffBase
                * RETRY_EXPONENTIAL_BASE, mAttemptCount.getAndAdd(1));
        return Math.min(retryTime, MAX_TIME_LIMIT_IN_SECS);
    }

    @Override
    public void resetCounter() {
        mAttemptCount.set(0);
    }
}
