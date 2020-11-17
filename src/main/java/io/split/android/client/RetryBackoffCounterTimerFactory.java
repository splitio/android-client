package io.split.android.client;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

// Class created  to allow unit testiing easily but also to reduce coupling
public class RetryBackoffCounterTimerFactory {
    public RetryBackoffCounterTimer create(SplitTaskExecutor splitTaskExecutor, int base) {
        return new RetryBackoffCounterTimer(splitTaskExecutor, new ReconnectBackoffCounter(base));
    }
}
