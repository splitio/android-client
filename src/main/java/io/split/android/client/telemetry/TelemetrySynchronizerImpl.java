package io.split.android.client.telemetry;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;

public class TelemetrySynchronizerImpl implements TelemetrySynchronizer {

    private final TelemetryTaskFactory mTaskFactory;
    private final RetryBackoffCounterTimer mConfigTimer;
    private final SplitTaskExecutor mTaskExecutor;
    private final long mTelemetrySyncPeriod;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);
    private final SplitTaskExecutionListener mTaskExecutionListener;

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
        mTaskExecutionListener = new SplitTaskExecutionListener() {
            @Override
            public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                    mIsSynchronizing.set(false);
                    stopStatsSynchronization();
                }
            }
        };
    }

    @Override
    public void synchronizeConfig() {
        if (mIsSynchronizing.get()) {
            mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask(), mTaskExecutionListener);
            mConfigTimer.start();
        }
    }

    @Override
    public void synchronizeStats() {
        if (statsTaskId != null) {
            mTaskExecutor.stopTask(statsTaskId);
        }
        statsTaskId = mTaskExecutor.schedule(
                mTaskFactory.getTelemetryStatsRecorderTask(),
                ServiceConstants.TELEMETRY_STATS_INITIAL_DELAY,
                mTelemetrySyncPeriod,
                mTaskExecutionListener
        );
    }

    @Override
    public void destroy() {
        mConfigTimer.stop();
        stopStatsSynchronization();
    }

    @Override
    public void flush() {
        if (mIsSynchronizing.get()) {
            mTaskExecutor.submit(mTaskFactory.getTelemetryStatsRecorderTask(), mTaskExecutionListener);
        }
    }

    private void stopStatsSynchronization() {
        if (statsTaskId != null) {
            mTaskExecutor.stopTask(statsTaskId);
            statsTaskId = null;
        }
    }
}
