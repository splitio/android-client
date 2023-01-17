package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionUtils;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;

/**
 * {@link ProcessStrategy} that corresponds to OPTIMIZED Impressions mode.
 */
class OptimizedStrategy implements ProcessStrategy {

    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;

    public OptimizedStrategy(@NonNull ImpressionsObserver impressionsObserver,
                             @NonNull ImpressionsCounter impressionsCounter,
                             @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                             @NonNull SplitTaskExecutor taskExecutor,
                             @NonNull ImpressionsTaskFactory taskFactory) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mImpressionsCounter = checkNotNull(impressionsCounter);
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        Long previousTime = mImpressionsObserver.testAndSet(impression);
        impression = impression.withPreviousTime(previousTime);

        if (previousTimeIsValid(previousTime)) {
            mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        }

        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (shouldPushImpression(keyImpression)) {
            if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression)) {
                mTaskExecutor.submit(
                        mImpressionsTaskFactory.createImpressionsRecorderTask(),
                        mImpressionsSyncHelper);
            }
        }
    }

    private static boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime) != ImpressionUtils.truncateTimeframe(impression.time);
    }

    private static boolean previousTimeIsValid(Long previousTime) {
        return previousTime != null && previousTime != 0;
    }
}
