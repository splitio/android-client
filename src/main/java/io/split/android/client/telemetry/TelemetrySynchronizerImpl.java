package io.split.android.client.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetrySynchronizerImpl implements TelemetrySynchronizer {

    private final TelemetryTaskFactory mTaskFactory;
    private final RetryBackoffCounterTimer mConfigTimer;
    private final SplitTaskExecutor mTaskExecutor;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final long mTelemetrySyncPeriod;

    private String statsTaskId = null;
    private final SplitTaskExecutionListener mStatsSyncListener;

    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                     long telemetrySyncPeriod) {
        this(splitTaskExecutor,
                telemetryTaskFactory,
                new RetryBackoffCounterTimer(splitTaskExecutor,
                        new FixedIntervalBackoffCounter(ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS),
                        ServiceConstants.TELEMETRY_CONFIG_MAX_RETRY_ATTEMPTS),
                telemetryRuntimeProducer,
                telemetrySyncPeriod);
    }

    @VisibleForTesting
    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     @NonNull RetryBackoffCounterTimer configTimer,
                                     @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                     long telemetrySyncPeriod) {
        mTaskExecutor = checkNotNull(splitTaskExecutor);
        mTaskFactory = checkNotNull(telemetryTaskFactory);
        mConfigTimer = checkNotNull(configTimer);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mTelemetrySyncPeriod = telemetrySyncPeriod;
        mStatsSyncListener = new TelemetrySyncTaskExecutionListener(mTelemetryRuntimeProducer, SplitTaskType.TELEMETRY_STATS_TASK, OperationType.TELEMETRY);
    }

    @Override
    public void synchronizeConfig() {
        mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask(),
                new TelemetrySyncTaskExecutionListener(mTelemetryRuntimeProducer, SplitTaskType.TELEMETRY_CONFIG_TASK, OperationType.TELEMETRY));
        mConfigTimer.start();
    }

    @Override
    public void synchronizeStats() {
        statsTaskId = mTaskExecutor.schedule(
                mTaskFactory.getTelemetryStatsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mTelemetrySyncPeriod,
                mStatsSyncListener
        );
    }

    @Override
    public void destroy() {
        mConfigTimer.stop();
        stopStatsSynchronization();
    }

    @Override
    public void flush() {
        mTaskExecutor.submit(mTaskFactory.getTelemetryStatsRecorderTask(), mStatsSyncListener);
    }

    private void stopStatsSynchronization() {
        if (statsTaskId != null) {
            mTaskExecutor.stopTask(statsTaskId);
            statsTaskId = null;
        }
    }
}
