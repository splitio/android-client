package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

class NoneTracker implements PeriodicTracker {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;
    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;
    private final RetryBackoffCounterTimer mImpressionsCountRetryTimer;
    private final RetryBackoffCounterTimer mUniqueKeysRetryTimer;
    private String mImpressionsRecorderCountTaskId;
    private String mUniqueKeysRecorderTaskId;
    private final int mImpressionsCounterRefreshRate;
    private final int mUniqueKeysRefreshRate;
    private final AtomicBoolean mTrackingIsEnabled;
    private final AtomicBoolean mIsSynchronizingMtks = new AtomicBoolean(true);
    private final AtomicBoolean mIsSynchronizingCounts = new AtomicBoolean(true);
    private final SplitTaskExecutionListener mMtkTaskExecutionListener;
    private final SplitTaskExecutionListener mCountTaskExecutionListener;

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
        mMtkTaskExecutionListener = new DoNotRetryListener(mIsSynchronizingMtks);
        mCountTaskExecutionListener = new DoNotRetryListener(mIsSynchronizingCounts);
    }

    @Override
    public void flush() {
        flushImpressionsCount();
        flushUniqueKeys();
    }

    @Override
    public void startPeriodicRecording() {
        if (mIsSynchronizingCounts.get()) {
            scheduleImpressionsCountRecorderTask();
        }

        if (mIsSynchronizingMtks.get()) {
            scheduleUniqueKeysRecorderTask();
        }
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        saveUniqueKeys();
        stopCountRecording();
        stopMtkRecording();
    }

    private void stopCountRecording() {
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
    }

    private void stopMtkRecording() {
        mTaskExecutor.stopTask(mUniqueKeysRecorderTaskId);
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
    }

    private void flushImpressionsCount() {
        mImpressionsCountRetryTimer.setTask(new SplitTaskSerialWrapper(
                mTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()),
                mTaskFactory.createImpressionsCountRecorderTask()));
        mImpressionsCountRetryTimer.start();
    }

    private void flushUniqueKeys() {
        mUniqueKeysRetryTimer.setTask(new SplitTaskSerialWrapper(
                mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()),
                mTaskFactory.createUniqueImpressionsRecorderTask()));
        mUniqueKeysRetryTimer.start();
    }

    private void scheduleImpressionsCountRecorderTask() {
        if (mImpressionsRecorderCountTaskId != null) {
            mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
        }
        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsCounterRefreshRate,
                mCountTaskExecutionListener);
    }

    private void scheduleUniqueKeysRecorderTask() {
        if (mUniqueKeysRecorderTaskId != null) {
            mTaskExecutor.stopTask(mUniqueKeysRecorderTaskId);
        }
        mUniqueKeysRecorderTaskId = mTaskExecutor.schedule(
                mTaskFactory.createUniqueImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mUniqueKeysRefreshRate,
                mMtkTaskExecutionListener);
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

    private static class DoNotRetryListener implements SplitTaskExecutionListener {

        private final AtomicBoolean mFlag;

        DoNotRetryListener(AtomicBoolean flag) {
            mFlag = flag;
        }

        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                mFlag.compareAndSet(true, false);
            }
        }
    }
}
