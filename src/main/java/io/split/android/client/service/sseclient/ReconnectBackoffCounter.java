package io.split.android.client.service.sseclient;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectBackoffCounter {
    private final static int MAX_TIME_LIMIT = 2;
    private final static int RETRY_EXPONENTIAL_BASE = 2;
    private final int mBackoffBase;
    private AtomicLong mAttemptCount;

    public ReconnectBackoffCounter(int mBackoffBase) {
        this.mBackoffBase = mBackoffBase;
        mAttemptCount = new AtomicLong(0);
    }

    public long getNextRetryTime() {
        long retryTime = (long) Math.pow(mBackoffBase
                * RETRY_EXPONENTIAL_BASE, mAttemptCount.getAndAdd(1));
        return Math.min(retryTime, MAX_TIME_LIMIT);
    }

    public void resetCounter() {
        mAttemptCount.set(0);
    }
}
