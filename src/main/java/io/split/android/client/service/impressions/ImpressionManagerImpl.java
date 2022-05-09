package io.split.android.client.service.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.service.synchronizer.RecorderSyncHelperImpl;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionManagerImpl implements ImpressionManager {

    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mSplitTaskFactory;
    private String mImpressionsRecorderTaskId;
    private String mImpressionsRecorderCountTaskId;
    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final ImpressionManagerConfig mImpressionManagerConfig;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;

    public ImpressionManagerImpl(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull ImpressionsTaskFactory splitTaskFactory,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull PersistentImpressionsStorage persistentImpressionsStorage,
                                 @NonNull ImpressionManagerConfig impressionManagerConfig) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mImpressionManagerConfig = checkNotNull(impressionManagerConfig);

        mImpressionsObserver = new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        mImpressionsCounter = new ImpressionsCounter();

        mImpressionsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                persistentImpressionsStorage,
                impressionManagerConfig.impressionsQueueSize,
                impressionManagerConfig.impressionsChunkSize,
                mTaskExecutor
        );
    }

    @Override
    public void pushImpression(Impression impression) {
        impression = impression.withPreviousTime(mImpressionsObserver.testAndSet(impression));
        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (isOptimizedImpressionsMode()) {
            mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        }

        if (!isOptimizedImpressionsMode() || shouldPushImpression(keyImpression)) {
            if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(keyImpression)) {
                mTaskExecutor.submit(
                        mSplitTaskFactory.createImpressionsRecorderTask(),
                        mImpressionsSyncHelper);
            }

            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
        } else {
            mTelemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1);
        }
    }

    @Override
    public void flush() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        flushImpressionsCount();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleImpressionsRecorderTask();
        scheduleImpressionsCountRecorderTask();
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
    }

    private boolean isOptimizedImpressionsMode() {
        return ImpressionsMode.OPTIMIZED.equals(mImpressionManagerConfig.impressionsMode);
    }

    private boolean isNoneImpressionsMode() {
//        return ImpressionsMode.NONE.equals(mImpressionsMode); TODO
        return false;
    }

    private void scheduleImpressionsRecorderTask() {
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionManagerConfig.impressionsRefreshRate, mImpressionsSyncHelper);
    }

    private void scheduleImpressionsCountRecorderTask() {
        if (!isOptimizedImpressionsMode()) {
            return;
        }

        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionManagerConfig.impressionsCounterRefreshRate,
                null);
    }

    private boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime) != ImpressionUtils.truncateTimeframe(impression.time);
    }

    private void flushImpressionsCount() {
        if (!isOptimizedImpressionsMode()) {
            return;
        }
        List<SplitTaskBatchItem> enqueued = new ArrayList<>();
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null));
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createImpressionsCountRecorderTask(), null));
        mTaskExecutor.executeSerially(enqueued);
    }

    private void saveImpressionsCount() {
        if (!isOptimizedImpressionsMode()) {
            return;
        }
        mTaskExecutor.submit(
                mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
    }

    public static class ImpressionManagerConfig {

        private final long impressionsRefreshRate;
        private final long impressionsCounterRefreshRate;
        private final ImpressionsMode impressionsMode;
        private final int impressionsQueueSize;
        private final long impressionsChunkSize;

        public ImpressionManagerConfig(long impressionsRefreshRate,
                                       long impressionsCounterRefreshRate,
                                       ImpressionsMode impressionsMode,
                                       int impressionsQueueSize,
                                       long impressionsChunkSize) {
            this.impressionsRefreshRate = impressionsRefreshRate;
            this.impressionsCounterRefreshRate = impressionsCounterRefreshRate;
            this.impressionsMode = impressionsMode;
            this.impressionsQueueSize = impressionsQueueSize;
            this.impressionsChunkSize = impressionsChunkSize;
        }
    }
}
