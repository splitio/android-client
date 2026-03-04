package io.split.android.client;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.backoff.FixedIntervalBackoffCounter;
import io.split.android.client.backoff.ExponentialBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

public class RetryBackoffCounterTimerFactory {
    public RetryBackoffCounterTimer create(SplitTaskExecutor splitTaskExecutor, int base) {
        return new RetryBackoffCounterTimer(splitTaskExecutor, new ExponentialBackoffCounter(base));
    }

    public RetryBackoffCounterTimer createWithFixedInterval(SplitTaskExecutor splitTaskExecutor, int retryIntervalInSeconds, int maxAttempts) {
        return new RetryBackoffCounterTimer(splitTaskExecutor,
                new FixedIntervalBackoffCounter(retryIntervalInSeconds), maxAttempts);
    }
}
