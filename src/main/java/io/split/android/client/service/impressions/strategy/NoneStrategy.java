package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;

/**
 * {@link ProcessStrategy} that corresponds to NONE Impressions mode.
 */
class NoneStrategy implements ProcessStrategy, OptionalPersistence {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mTaskFactory;

    private final ImpressionsCounter mImpressionsCounter;
    private final UniqueKeysTracker mUniqueKeysTracker;
    private final AtomicBoolean mEnablePersistence;

    public NoneStrategy(@NonNull SplitTaskExecutor taskExecutor,
                        @NonNull ImpressionsTaskFactory taskFactory,
                        @NonNull ImpressionsCounter impressionsCounter,
                        @NonNull UniqueKeysTracker uniqueKeysTracker,
                        boolean enablePersistence) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mTaskFactory = checkNotNull(taskFactory);

        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);

        mEnablePersistence = new AtomicBoolean(enablePersistence);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        mUniqueKeysTracker.track(impression.key(), impression.split());

        if (mEnablePersistence.get() && mUniqueKeysTracker.isFull()) {
            saveUniqueKeys();
        }
    }

    private void saveUniqueKeys() {
        mTaskExecutor.submit(
                mTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
    }

    @Override
    public void enablePersistence(boolean enable) {
        mEnablePersistence.set(enable);
    }
}
