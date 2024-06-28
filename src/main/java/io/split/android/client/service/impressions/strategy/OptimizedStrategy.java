package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionUtils;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.observer.ImpressionsObserver;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
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
    private final PeriodicTracker mOptimizedTracker;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);
    private final long mImpressionsDedupeTimeInterval;
    /** @noinspection FieldCanBeLocal*/
    private final SplitTaskExecutionListener mTaskExecutionListener = new SplitTaskExecutionListener() {
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            // this listener intercepts impressions recording task
            if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                    mIsSynchronizing.compareAndSet(true, false);
                    mOptimizedTracker.stopPeriodicRecording();
                }
            }
        }
    };

    OptimizedStrategy(@NonNull ImpressionsObserver impressionsObserver,
                      @NonNull ImpressionsCounter impressionsCounter,
                      @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                      @NonNull SplitTaskExecutor taskExecutor,
                      @NonNull ImpressionsTaskFactory taskFactory,
                      @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                      @NonNull RetryBackoffCounterTimer impressionsRetryTimer,
                      @NonNull RetryBackoffCounterTimer impressionsCountRetryTimer,
                      int impressionsRefreshRate,
                      int impressionsCounterRefreshRate,
                      boolean isTrackingEnabled,
                      long impressionsDedupeTimeInterval) {
        this(impressionsObserver,
                impressionsCounter,
                impressionsSyncHelper,
                taskExecutor,
                taskFactory,
                telemetryRuntimeProducer,
                isTrackingEnabled,
                new OptimizedTracker(impressionsCounter,
                        impressionsSyncHelper,
                        taskExecutor,
                        taskFactory,
                        impressionsRetryTimer,
                        impressionsCountRetryTimer,
                        impressionsRefreshRate,
                        impressionsCounterRefreshRate,
                        isTrackingEnabled),
                impressionsDedupeTimeInterval);
    }

    @VisibleForTesting
    OptimizedStrategy(@NonNull ImpressionsObserver impressionsObserver,
                      @NonNull ImpressionsCounter impressionsCounter,
                      @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                      @NonNull SplitTaskExecutor taskExecutor,
                      @NonNull ImpressionsTaskFactory taskFactory,
                      @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                      boolean isTrackingEnabled,
                      @NonNull PeriodicTracker tracker,
                      long impressionsDedupeTimeInterval) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mImpressionsCounter = checkNotNull(impressionsCounter);
        RecorderSyncHelper<KeyImpression> syncHelper = checkNotNull(impressionsSyncHelper);
        syncHelper.addListener(mTaskExecutionListener);
        mImpressionsSyncHelper = syncHelper;
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mTrackingIsEnabled = new AtomicBoolean(isTrackingEnabled);
        mOptimizedTracker = checkNotNull(tracker);
        mImpressionsDedupeTimeInterval = impressionsDedupeTimeInterval;
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
            if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression) && mIsSynchronizing.get()) {
                mTaskExecutor.submit(
                        mImpressionsTaskFactory.createImpressionsRecorderTask(),
                        mImpressionsSyncHelper);
            }

            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
        } else {
            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1);
        }
    }

    private boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime, mImpressionsDedupeTimeInterval) != ImpressionUtils.truncateTimeframe(impression.time, mImpressionsDedupeTimeInterval);
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
        if (mIsSynchronizing.get()) {
            mOptimizedTracker.startPeriodicRecording();
        }
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
