package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;

/**
 * {@link ProcessStrategy} that corresponds to NONE Impressions mode.
 */
class NoneStrategy implements ProcessStrategy {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;

    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;

    public NoneStrategy(@NonNull SplitTaskExecutor taskExecutor,
                        @NonNull ImpressionsTaskFactory taskFactory,
                        @NonNull ImpressionsCounter impressionsCounter,
                        @NonNull UniqueKeysTracker uniqueKeysTracker) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mTaskFactory = checkNotNull(taskFactory);

        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        mUniqueKeysTracker.track(impression.key(), impression.split());

        if (mUniqueKeysTracker.isFull()) {
            saveUniqueKeys();
        }
    }

    private void saveUniqueKeys() {
        mTaskExecutor.submit(
                mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
    }
}
