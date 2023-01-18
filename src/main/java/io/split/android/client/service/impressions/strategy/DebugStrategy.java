package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

/**
 * {@link ProcessStrategy} that corresponds to DEBUG Impressions mode.
 */
class DebugStrategy implements ProcessStrategy {

    private final ImpressionsObserver mImpressionsObserver;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;

    public DebugStrategy(@NonNull ImpressionsObserver impressionsObserver,
                         @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                         @NonNull SplitTaskExecutor taskExecutor,
                         @NonNull ImpressionsTaskFactory taskFactory,
                         @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        Long previousTime = mImpressionsObserver.testAndSet(impression);
        impression = impression.withPreviousTime(previousTime);

        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression)) {
            mTaskExecutor.submit(
                    mImpressionsTaskFactory.createImpressionsRecorderTask(),
                    mImpressionsSyncHelper);
        }

        mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
    }
}
