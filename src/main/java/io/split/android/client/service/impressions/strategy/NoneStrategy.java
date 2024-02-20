package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

/**
 * {@link ProcessStrategy} that corresponds to {@link ImpressionsMode#NONE} Impressions mode.
 */
class NoneStrategy implements ProcessStrategy {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;
    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;
    private final AtomicBoolean mTrackingIsEnabled;
    private final PeriodicTracker mNoneTracker;

    NoneStrategy(@NonNull SplitTaskExecutor taskExecutor,
                 @NonNull ImpressionsTaskFactory taskFactory,
                 @NonNull ImpressionsCounter impressionsCounter,
                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                 @NonNull RetryBackoffCounterTimer impressionsCountRetryTimer,
                 @NonNull RetryBackoffCounterTimer uniqueKeysRetryTimer,
                 int impressionsCounterRefreshRate,
                 int uniqueKeysRefreshRate,
                 boolean trackingIsEnabled) {
        this(taskExecutor,
                taskFactory,
                impressionsCounter,
                uniqueKeysTracker,
                trackingIsEnabled,
                new NoneTracker(
                        taskExecutor,
                        taskFactory,
                        impressionsCounter,
                        uniqueKeysTracker,
                        impressionsCountRetryTimer,
                        uniqueKeysRetryTimer,
                        impressionsCounterRefreshRate,
                        uniqueKeysRefreshRate,
                        trackingIsEnabled));
    }

    @VisibleForTesting
    NoneStrategy(@NonNull SplitTaskExecutor taskExecutor,
                 @NonNull ImpressionsTaskFactory taskFactory,
                 @NonNull ImpressionsCounter impressionsCounter,
                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                 boolean trackingIsEnabled,
                 @NonNull PeriodicTracker tracker) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mTaskFactory = checkNotNull(taskFactory);
        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);
        mTrackingIsEnabled = new AtomicBoolean(trackingIsEnabled);
        mNoneTracker = checkNotNull(tracker);
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
        mNoneTracker.flush();
    }

    @Override
    public void startPeriodicRecording() {
        mNoneTracker.startPeriodicRecording();
    }

    @Override
    public void stopPeriodicRecording() {
        mNoneTracker.stopPeriodicRecording();
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
        mNoneTracker.enableTracking(enable);
    }

    private void saveUniqueKeys() {
        if (mTrackingIsEnabled.get()) {
            mTaskExecutor.submit(
                    mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
        }
    }
}
