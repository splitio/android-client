package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;

class OptimizedTracker implements PeriodicTracker {

    private final ImpressionsCounter mImpressionsCounter;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;
    private final RetryBackoffCounterTimer mRetryTimer;
    private final RetryBackoffCounterTimer mImpressionsCountRetryTimer;
    private final int mImpressionsRefreshRate;
    private final int mImpressionsCounterRefreshRate;
    private String mImpressionsRecorderTaskId;
    private String mImpressionsRecorderCountTaskId;
    private final AtomicBoolean mTrackingIsEnabled;

    OptimizedTracker(@NonNull ImpressionsCounter impressionsCounter,
                     @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                     @NonNull SplitTaskExecutor taskExecutor,
                     @NonNull ImpressionsTaskFactory taskFactory,
                     @NonNull RetryBackoffCounterTimer impressionsRetryTimer,
                     @NonNull RetryBackoffCounterTimer impressionsCountRetryTimer,
                     int impressionsRefreshRate,
                     int impressionsCounterRefreshRate,
                     boolean isTrackingEnabled) {
        mImpressionsCounter = checkNotNull(impressionsCounter);
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mRetryTimer = checkNotNull(impressionsRetryTimer);
        mImpressionsCountRetryTimer = checkNotNull(impressionsCountRetryTimer);
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mTrackingIsEnabled = new AtomicBoolean(isTrackingEnabled);
    }

    @Override
    public void flush() {
        flushImpressions();
        flushImpressionsCount();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleImpressionsRecorderTask();
        scheduleImpressionsCountRecorderTask();
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
    }

    private void flushImpressions() {
        mRetryTimer.setTask(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        mRetryTimer.start();
    }

    private void flushImpressionsCount() {
        SplitTask task;
        if (mTrackingIsEnabled.get()) {
            task = new SplitTaskSerialWrapper(
                    mImpressionsTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()),
                    mImpressionsTaskFactory.createImpressionsCountRecorderTask());

        } else {
            task = mImpressionsTaskFactory.createImpressionsCountRecorderTask();
        }

        mImpressionsCountRetryTimer.setTask(task);
        mImpressionsCountRetryTimer.start();
    }

    private void scheduleImpressionsRecorderTask() {
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsRefreshRate,
                mImpressionsSyncHelper);
    }

    private void scheduleImpressionsCountRecorderTask() {
        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mImpressionsTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsCounterRefreshRate,
                null);
    }

    private void saveImpressionsCount() {
        if (mTrackingIsEnabled.get()) {
            mTaskExecutor.submit(
                    mImpressionsTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
        }
    }
}
