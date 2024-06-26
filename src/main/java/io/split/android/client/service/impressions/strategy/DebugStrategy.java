package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.observer.ImpressionsObserver;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
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
    private final PeriodicTracker mDebugTracker;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);
    /** @noinspection FieldCanBeLocal*/
    private final SplitTaskExecutionListener mTaskExecutionListener = new SplitTaskExecutionListener() {
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            // this listener intercepts impressions recording task
            if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                    mIsSynchronizing.compareAndSet(true, false);
                    mDebugTracker.stopPeriodicRecording();
                }
            }
        }
    };

    DebugStrategy(@NonNull ImpressionsObserver impressionsObserver,
                  @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                  @NonNull SplitTaskExecutor taskExecutor,
                  @NonNull ImpressionsTaskFactory taskFactory,
                  @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                  @NonNull RetryBackoffCounterTimer retryTimer,
                  int impressionsRefreshRate) {
        this(impressionsObserver,
                impressionsSyncHelper,
                taskExecutor,
                taskFactory,
                telemetryRuntimeProducer,
                new DebugTracker(impressionsSyncHelper, taskExecutor, taskFactory, retryTimer, impressionsRefreshRate));
    }

    @VisibleForTesting
    DebugStrategy(@NonNull ImpressionsObserver impressionsObserver,
                  @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                  @NonNull SplitTaskExecutor taskExecutor,
                  @NonNull ImpressionsTaskFactory taskFactory,
                  @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                  @NonNull PeriodicTracker tracker) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        RecorderSyncHelper<KeyImpression> syncHelper = checkNotNull(impressionsSyncHelper);
        syncHelper.addListener(mTaskExecutionListener);
        mImpressionsSyncHelper = syncHelper;
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mDebugTracker = checkNotNull(tracker);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        @Nullable Long previousTime = mImpressionsObserver.testAndSet(impression);
        impression = impression.withPreviousTime(previousTime);
        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression) && mIsSynchronizing.get()) {
            mTaskExecutor.submit(
                    mImpressionsTaskFactory.createImpressionsRecorderTask(),
                    mImpressionsSyncHelper);
        }

        mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
    }

    @Override
    public void flush() {
        mDebugTracker.flush();
    }

    @Override
    public void startPeriodicRecording() {
        if (mIsSynchronizing.get()) {
            mDebugTracker.startPeriodicRecording();
        }
    }

    @Override
    public void stopPeriodicRecording() {
        mDebugTracker.stopPeriodicRecording();
    }

    @Override
    public void enableTracking(boolean enable) {
        mDebugTracker.enableTracking(enable);
    }
}
