package io.split.android.client.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;

// TODO: implement
public class TelemetrySynchronizerImpl implements TelemetrySynchronizer {

    private static final int RETRY_INTERVAL_SECONDS = 1;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final TelemetryTaskFactory mTaskFactory;
    private final RetryBackoffCounterTimer mConfigTimer;

    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory) {
        SplitTaskExecutor mTaskExecutor = checkNotNull(splitTaskExecutor);
        mTaskFactory = checkNotNull(telemetryTaskFactory);
        mConfigTimer = new RetryBackoffCounterTimer(mTaskExecutor,
                new FixedIntervalBackoffCounter(RETRY_INTERVAL_SECONDS),
                MAX_RETRY_ATTEMPTS);
    }

    @Override
    public void synchronizeConfig() {
        mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask(), null);
        mConfigTimer.start();
    }

    @Override
    public void synchronizeStats() {
        //TODO
    }

    @Override
    public void destroy() {
        mConfigTimer.stop();
    }
}
