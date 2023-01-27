package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;

/**
 * {@link ProcessStrategy} that corresponds to NONE Impressions mode.
 */
class NoneStrategy implements ProcessStrategy {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;

    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;

    //////
    private final RetryBackoffCounterTimer mImpressionsCountRetryTimer;
    private final RetryBackoffCounterTimer mUniqueKeysRetryTimer;
    private String mImpressionsRecorderCountTaskId;
    private String mUniqueKeysRecorderTaskId;
    private final int mImpressionsCounterRefreshRate;
    private final int mUniqueKeysRefreshRate;
    private final boolean mTrackingIsEnabled;

    public NoneStrategy(@NonNull SplitTaskExecutor taskExecutor,
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
        mTrackingIsEnabled = trackingIsEnabled;
    }

    @Override
    public void apply(@NonNull Impression impression) {
        mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        mUniqueKeysTracker.track(impression.key(), impression.split());

        if (mUniqueKeysTracker.isFull()) {
            saveUniqueKeys();
        }
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
        if (mTrackingIsEnabled) {
            mTaskExecutor.submit(
                    mTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
        }
    }

    private void saveUniqueKeys() {
        if (mTrackingIsEnabled) {
            mTaskExecutor.submit(
                    mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
        }
    }
}
