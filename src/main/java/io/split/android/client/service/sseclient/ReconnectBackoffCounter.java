package io.split.android.client.service.sseclient;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectBackoffCounter implements BackoffCounter {
    private final static int MAX_TIME_LIMIT_IN_SECS = 1800; // 30 minutes (30 * 60)
    private final static int RETRY_EXPONENTIAL_BASE = 2;
    private final int mBackoffBase;
    private final AtomicLong mAttemptCount;
    private final int mMaxTimeLimit;

    /**
     * @param backoffBase the base of the backoff in seconds
     */
    public ReconnectBackoffCounter(int backoffBase) {
        this(backoffBase, MAX_TIME_LIMIT_IN_SECS);
    }

    /**
     * @param backoffBase the base of the backoff in seconds
     * @param maxTimeLimit the maximum time limit in seconds
     */
    public ReconnectBackoffCounter(int backoffBase, int maxTimeLimit) {
        mBackoffBase = backoffBase;
        mAttemptCount = new AtomicLong(0);
        mMaxTimeLimit = maxTimeLimit;
    }

    @Override
    public long getNextRetryTime() {
        long retryTime = (long) Math.pow(mBackoffBase
                * RETRY_EXPONENTIAL_BASE, mAttemptCount.getAndAdd(1));

        return Math.min(retryTime, mMaxTimeLimit);
    }

    @Override
    public void resetCounter() {
        mAttemptCount.set(0);
    }
}
