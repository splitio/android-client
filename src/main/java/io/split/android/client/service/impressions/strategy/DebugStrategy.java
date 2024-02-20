package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.ImpressionsMode;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

/**
 * {@link ProcessStrategy} that corresponds to {@link ImpressionsMode#DEBUG} Impressions mode.
 */
class DebugStrategy implements ProcessStrategy {

    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;
    private final PeriodicTracker mDebugTracker;

    DebugStrategy(@NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                  @NonNull SplitTaskExecutor taskExecutor,
                  @NonNull ImpressionsTaskFactory taskFactory,
                  @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                  @NonNull RetryBackoffCounterTimer retryTimer,
                  int impressionsRefreshRate) {
        this(
                impressionsSyncHelper,
                taskExecutor,
                taskFactory,
                telemetryRuntimeProducer,
                new DebugTracker(impressionsSyncHelper, taskExecutor, taskFactory, retryTimer, impressionsRefreshRate));
    }

    @VisibleForTesting
    DebugStrategy(@NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                  @NonNull SplitTaskExecutor taskExecutor,
                  @NonNull ImpressionsTaskFactory taskFactory,
                  @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                  @NonNull PeriodicTracker tracker) {
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mDebugTracker = checkNotNull(tracker);
    }

    @Override
    public void apply(@NonNull Impression impression) {
        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression)) {
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
        mDebugTracker.startPeriodicRecording();
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
