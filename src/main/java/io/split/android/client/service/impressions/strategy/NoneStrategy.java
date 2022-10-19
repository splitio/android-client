package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;

/**
 * {@link ProcessStrategy} that corresponds to NONE {@link ImpressionsMode}
 */
class NoneStrategy implements ProcessStrategy {

    private final ImpressionsObserver mImpressionsObserver;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;

    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;

    public NoneStrategy(@NonNull ImpressionsObserver impressionsObserver,
                        @NonNull SplitTaskExecutor taskExecutor,
                        @NonNull ImpressionsTaskFactory taskFactory,
                        @NonNull ImpressionsCounter impressionsCounter,
                        @NonNull UniqueKeysTracker uniqueKeysTracker) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mTaskExecutor = checkNotNull(taskExecutor);
        mTaskFactory = checkNotNull(taskFactory);

        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        Long previousTime = mImpressionsObserver.testAndSet(impression);
        impression = impression.withPreviousTime(previousTime);
        if (previousTimeIsValid(previousTime)) {
            mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        }

        mUniqueKeysTracker.track(impression.key(), impression.split());

        if (mUniqueKeysTracker.size() >= ServiceConstants.MAX_UNIQUE_KEYS_IN_MEMORY) {
            saveUniqueKeys();
        }
    }

    private static boolean previousTimeIsValid(Long previousTime) {
        return previousTime != null && previousTime != 0;
    }

    private void saveUniqueKeys() {
        mTaskExecutor.submit(
                mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
    }
}
