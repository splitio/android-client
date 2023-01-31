package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

class NoneTracker implements PeriodicTracker {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;
    private final RetryBackoffCounterTimer mImpressionsCountRetryTimer;
    private final RetryBackoffCounterTimer mUniqueKeysRetryTimer;
    private String mImpressionsRecorderCountTaskId;
    private String mUniqueKeysRecorderTaskId;
    private final int mImpressionsCounterRefreshRate;
    private final int mUniqueKeysRefreshRate;
    private final AtomicBoolean mTrackingIsEnabled;
    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;

    NoneTracker(@NonNull SplitTaskExecutor taskExecutor,
                 @NonNull ImpressionsTaskFactory taskFactory,
                 @NonNull ImpressionsCounter impressionsCounter,
                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                 @NonNull RetryBackoffCounterTimer impressionsCountRetryTimer,
                 @NonNull RetryBackoffCounterTimer uniqueKeysRetryTimer,
                 int impressionsCounterRefreshRate,
                 int uniqueKeysRefreshRate,
                 boolean trackingIsEnabled) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mTaskFactory = checkNotNull(taskFactory);

        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);

        mImpressionsCountRetryTimer = checkNotNull(impressionsCountRetryTimer);
        mUniqueKeysRetryTimer = checkNotNull(uniqueKeysRetryTimer);
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mUniqueKeysRefreshRate = uniqueKeysRefreshRate;
        mTrackingIsEnabled = new AtomicBoolean(trackingIsEnabled);
    }

    @Override
    public void flush() {
        flushImpressionsCount();
        flushUniqueKeys();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleImpressionsCountRecorderTask();
        scheduleUniqueKeysRecorderTask();
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        saveUniqueKeys();
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
        mTaskExecutor.stopTask(mUniqueKeysRecorderTaskId);
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
    }

    private void flushImpressionsCount() {
        SplitTask task;
        if (mTrackingIsEnabled.get()) {
            task = new SplitTaskSerialWrapper(
                    mTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()),
                    mTaskFactory.createImpressionsCountRecorderTask());
        } else {
            task = mTaskFactory.createImpressionsCountRecorderTask();
        }
        mImpressionsCountRetryTimer.setTask(task);
        mImpressionsCountRetryTimer.start();
    }

    private void flushUniqueKeys() {
        SplitTask task;
        if (mTrackingIsEnabled.get()) {
            task = new SplitTaskSerialWrapper(
                    mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()),
                    mTaskFactory.createUniqueImpressionsRecorderTask());
        } else {
            task = mTaskFactory.createUniqueImpressionsRecorderTask();
        }
        mUniqueKeysRetryTimer.setTask(task);
        mUniqueKeysRetryTimer.start();
    }

    private void scheduleImpressionsCountRecorderTask() {
        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsCounterRefreshRate,
                null);
    }

    private void scheduleUniqueKeysRecorderTask() {
        mUniqueKeysRecorderTaskId = mTaskExecutor.schedule(
                mTaskFactory.createUniqueImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mUniqueKeysRefreshRate,
                null);
    }

    private void saveImpressionsCount() {
        if (mTrackingIsEnabled.get()) {
            mTaskExecutor.submit(
                    mTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
        }
    }

    private void saveUniqueKeys() {
        if (mTrackingIsEnabled.get()) {
            mTaskExecutor.submit(
                    mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
        }
    }
}
