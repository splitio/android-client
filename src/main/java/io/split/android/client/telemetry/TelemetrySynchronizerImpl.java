package io.split.android.client.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;

public class TelemetrySynchronizerImpl implements TelemetrySynchronizer {

    private static final int RETRY_INTERVAL_SECONDS = 1;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final TelemetryTaskFactory mTaskFactory;
    private final RetryBackoffCounterTimer mConfigTimer;
    private final SplitTaskExecutor mTaskExecutor;
    private final long mTelemetrySyncPeriod;

    private String statsTaskId = null;

    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     long telemetrySyncPeriod) {
        this(splitTaskExecutor,
                telemetryTaskFactory,
                new RetryBackoffCounterTimer(splitTaskExecutor,
                        new FixedIntervalBackoffCounter(RETRY_INTERVAL_SECONDS),
                        MAX_RETRY_ATTEMPTS),
                telemetrySyncPeriod);
    }

    @VisibleForTesting
    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     @NonNull RetryBackoffCounterTimer configTimer,
                                     long telemetrySyncPeriod) {
        mTaskExecutor = checkNotNull(splitTaskExecutor);
        mTaskFactory = checkNotNull(telemetryTaskFactory);
        mConfigTimer = checkNotNull(configTimer);
        mTelemetrySyncPeriod = telemetrySyncPeriod;
    }

    @Override
    public void synchronizeConfig() {
        mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask());
        mConfigTimer.start();
    }

    @Override
    public void synchronizeStats() {
        statsTaskId = mTaskExecutor.schedule(
                mTaskFactory.getTelemetryStatsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mTelemetrySyncPeriod,
                null
        );
    }

    @Override
    public void destroy() {
        mConfigTimer.stop();
        stopStatsSynchronization();
    }

    @Override
    public void flush() {
        mTaskExecutor.submit(mTaskFactory.getTelemetryStatsRecorderTask(), null);
    }

    private void stopStatsSynchronization() {
        if (statsTaskId != null) {
            mTaskExecutor.stopTask(statsTaskId);
            statsTaskId = null;
        }
    }
}
