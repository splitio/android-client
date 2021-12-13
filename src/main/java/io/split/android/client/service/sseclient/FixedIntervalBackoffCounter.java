package io.split.android.client.service.sseclient;

import java.util.concurrent.TimeUnit;

public class FixedIntervalBackoffCounter implements BackoffCounter {

    private final long mRetryInterval;

    public FixedIntervalBackoffCounter(long retryInterval, TimeUnit timeUnit) {
        mRetryInterval = TimeUnit.MILLISECONDS.convert(retryInterval, timeUnit);
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
