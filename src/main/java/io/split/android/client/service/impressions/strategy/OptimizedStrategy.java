package io.split.android.client.service.impressions.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.impressions.ImpressionUtils;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

/**
 * {@link ProcessStrategy} that corresponds to OPTIMIZED Impressions mode.
 */
class OptimizedStrategy implements ProcessStrategy {

    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    private final AtomicBoolean mTrackingIsEnabled;

    private final OptimizedTracker mOptimizedTracker;

    OptimizedStrategy(@NonNull ImpressionsObserver impressionsObserver,
                      @NonNull ImpressionsCounter impressionsCounter,
                      @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                      @NonNull SplitTaskExecutor taskExecutor,
                      @NonNull ImpressionsTaskFactory taskFactory,
                      @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,

                      ///////////
                      @NonNull RetryBackoffCounterTimer impressionsRetryTimer,
                      @NonNull RetryBackoffCounterTimer impressionsCountRetryTimer,
                      int impressionsRefreshRate,
                      int impressionsCounterRefreshRate,
                      boolean isTrackingEnabled) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mImpressionsCounter = checkNotNull(impressionsCounter);
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        RetryBackoffCounterTimer mRetryTimer = checkNotNull(impressionsRetryTimer);
        RetryBackoffCounterTimer mImpressionsCountRetryTimer = checkNotNull(impressionsCountRetryTimer);
        mTrackingIsEnabled = new AtomicBoolean(isTrackingEnabled);

        mOptimizedTracker = new OptimizedTracker(mImpressionsCounter,
                mImpressionsSyncHelper,
                mTaskExecutor,
                mImpressionsTaskFactory,
                mRetryTimer,
                mImpressionsCountRetryTimer,
                impressionsRefreshRate,
                impressionsCounterRefreshRate,
                isTrackingEnabled);
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

            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
        } else {
            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1);
        }
    }

    private static boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime) != ImpressionUtils.truncateTimeframe(impression.time);
    }

    private static boolean previousTimeIsValid(Long previousTime) {
        return previousTime != null && previousTime != 0;
    }

    @Override
    public void flush() {
        mOptimizedTracker.flush();
    }

    @Override
    public void startPeriodicRecording() {
        mOptimizedTracker.startPeriodicRecording();
    }

    @Override
    public void stopPeriodicRecording() {
        mOptimizedTracker.stopPeriodicRecording();
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
        mOptimizedTracker.enableTracking(enable);
    }
}
