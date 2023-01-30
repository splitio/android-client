package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;

class DebugTracker implements PeriodicTracker {

    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;

    private final RetryBackoffCounterTimer mRetryTimer;
    private final int mImpressionsRefreshRate;
    private String mImpressionsRecorderTaskId;

    DebugTracker(@NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                 @NonNull SplitTaskExecutor taskExecutor,
                 @NonNull ImpressionsTaskFactory taskFactory,
                 @NonNull RetryBackoffCounterTimer retryTimer,
                 int impressionsRefreshRate) {
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mRetryTimer = retryTimer;
        mImpressionsRefreshRate = impressionsRefreshRate;
    }

    @Override
    public void flush() {
        flushImpressions();
    }

    private void flushImpressions() {
        mRetryTimer.setTask(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        mRetryTimer.start();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleImpressionsRecorderTask();
    }

    private void scheduleImpressionsRecorderTask() {
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsRefreshRate,
                mImpressionsSyncHelper);
    }

    @Override
    public void stopPeriodicRecording() {
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
    }

    @Override
    public void enableTracking(boolean enable) {
        // no - op
    }
}
