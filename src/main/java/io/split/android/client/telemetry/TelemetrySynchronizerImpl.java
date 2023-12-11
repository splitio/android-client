package io.split.android.client.telemetry;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;

public class TelemetrySynchronizerImpl implements TelemetrySynchronizer {

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
                        new FixedIntervalBackoffCounter(ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS),
                        ServiceConstants.TELEMETRY_CONFIG_MAX_RETRY_ATTEMPTS),
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
        mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask(), null);
        mConfigTimer.start();
    }

    @Override
    public void synchronizeStats() {
        statsTaskId = mTaskExecutor.schedule(
                mTaskFactory.getTelemetryStatsRecorderTask(),
                ServiceConstants.TELEMETRY_STATS_INITIAL_DELAY,
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
