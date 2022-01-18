package io.split.android.client.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;
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
    private final long mTelemetrySyncPeriod;

    private String statsTaskId = null;
    private final TelemetrySyncTaskExecutionListener mStatsSyncListener;
    private final TelemetrySyncTaskExecutionListener mConfigSyncListener;

    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                     long telemetrySyncPeriod) {
        this(splitTaskExecutor,
                telemetryTaskFactory,
                new RetryBackoffCounterTimer(splitTaskExecutor,
                        new FixedIntervalBackoffCounter(ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS),
                        ServiceConstants.TELEMETRY_CONFIG_MAX_RETRY_ATTEMPTS),
                new TelemetrySyncTaskExecutionListenerFactoryImpl(telemetryRuntimeProducer),
                telemetrySyncPeriod);
    }

    @VisibleForTesting
    public TelemetrySynchronizerImpl(@NonNull SplitTaskExecutor splitTaskExecutor,
                                     @NonNull TelemetryTaskFactory telemetryTaskFactory,
                                     @NonNull RetryBackoffCounterTimer configTimer,
                                     @NonNull TelemetrySyncTaskExecutionListenerFactory telemetrySyncTaskExecutionListenerFactory,
                                     long telemetrySyncPeriod) {
        mTaskExecutor = checkNotNull(splitTaskExecutor);
        mTaskFactory = checkNotNull(telemetryTaskFactory);
        mConfigTimer = checkNotNull(configTimer);
        mTelemetrySyncPeriod = telemetrySyncPeriod;

        mStatsSyncListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.TELEMETRY_STATS_TASK, OperationType.TELEMETRY);
        mConfigSyncListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.TELEMETRY_CONFIG_TASK, OperationType.TELEMETRY);
    }

    @Override
    public void synchronizeConfig() {
        mConfigTimer.setTask(mTaskFactory.getTelemetryConfigRecorderTask(),
                mConfigSyncListener);
        mConfigTimer.start();
    }

    @Override
    public void synchronizeStats() {
        statsTaskId = mTaskExecutor.schedule(
                mTaskFactory.getTelemetryStatsRecorderTask(),
                ServiceConstants.TELEMETRY_STATS_INITIAL_DELAY,
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
