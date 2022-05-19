package io.split.android.client.service.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.FixedIntervalBackoffCounter;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
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
    private String mUniqueKeysRecorderTaskId;
    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final ImpressionManagerConfig mImpressionManagerConfig;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final UniqueKeysTracker mUniqueKeysTracker;
    private final RetryBackoffCounterTimer mUniqueKeysTimer;

    public ImpressionManagerImpl(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull ImpressionsTaskFactory splitTaskFactory,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull PersistentImpressionsStorage persistentImpressionsStorage,
                                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                                 @NonNull ImpressionManagerConfig impressionManagerConfig) {
        this(
                taskExecutor,
                splitTaskFactory,
                telemetryRuntimeProducer,
                uniqueKeysTracker,
                impressionManagerConfig,
                new RecorderSyncHelperImpl<>(
                        SplitTaskType.IMPRESSIONS_RECORDER,
                        persistentImpressionsStorage,
                        impressionManagerConfig.getImpressionsQueueSize(),
                        impressionManagerConfig.getImpressionsChunkSize(),
                        taskExecutor),
                new RetryBackoffCounterTimer(taskExecutor,
                        new FixedIntervalBackoffCounter(ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS),
                        ServiceConstants.UNIQUE_KEYS_MAX_RETRY_ATTEMPTS));
    }


    @VisibleForTesting
    public ImpressionManagerImpl(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull ImpressionsTaskFactory splitTaskFactory,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                                 @NonNull ImpressionManagerConfig impressionManagerConfig,
                                 @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                                 @NonNull RetryBackoffCounterTimer uniqueKeysTimer) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mImpressionManagerConfig = checkNotNull(impressionManagerConfig);

        mImpressionsObserver = new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        mImpressionsCounter = new ImpressionsCounter();
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);

        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);

        mUniqueKeysTimer = checkNotNull(uniqueKeysTimer);
    }

    @Override
    public void pushImpression(Impression impression) {
        impression = impression.withPreviousTime(mImpressionsObserver.testAndSet(impression));
        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
        if (shouldTrackImpressionsCount()) {
            mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        }

        if (isNoneImpressionsMode()) {
            mUniqueKeysTracker.track(impression.key(), impression.split());

            if (mUniqueKeysTracker.size() >= ServiceConstants.MAX_UNIQUE_KEYS_IN_MEMORY) {
                saveUniqueKeys();
            }
        }

        if (!shouldTrackImpressionsCount() || (!isNoneImpressionsMode() && shouldPushImpression(keyImpression))) {
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
        flushUniqueKeys();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleImpressionsRecorderTask();
        scheduleImpressionsCountRecorderTask();
        scheduleUniqueKeysRecorderTask();
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        saveUniqueKeys();
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
        mTaskExecutor.stopTask(mUniqueKeysRecorderTaskId);
    }

    private boolean isOptimizedImpressionsMode() {
        return mImpressionManagerConfig.getImpressionsMode().isOptimized();
    }

    private boolean isNoneImpressionsMode() {
        return mImpressionManagerConfig.getImpressionsMode().isNone();
    }

    private void scheduleImpressionsRecorderTask() {
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionManagerConfig.getImpressionsRefreshRate(),
                mImpressionsSyncHelper);
    }

    private void scheduleImpressionsCountRecorderTask() {
        if (!shouldTrackImpressionsCount()) {
            return;
        }

        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionManagerConfig.getImpressionsCounterRefreshRate(),
                null);
    }

    private void scheduleUniqueKeysRecorderTask() {
        if (!isNoneImpressionsMode()) {
            return;
        }

        mUniqueKeysRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createUniqueImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionManagerConfig.getUniqueKeysRefreshRate(),
                null);
    }

    private boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime) != ImpressionUtils.truncateTimeframe(impression.time);
    }

    private void flushImpressionsCount() {
        if (!shouldTrackImpressionsCount()) {
            return;
        }
        List<SplitTaskBatchItem> enqueued = new ArrayList<>();
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null));
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createImpressionsCountRecorderTask(), null));
        mTaskExecutor.executeSerially(enqueued);
    }

    private void flushUniqueKeys() {
        if (!isNoneImpressionsMode()) {
            return;
        }
        mUniqueKeysTimer.setTask(new SplitTaskSerialWrapper(
                mSplitTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()),
                mSplitTaskFactory.createUniqueImpressionsRecorderTask()));
        mUniqueKeysTimer.start();
    }

    private void saveImpressionsCount() {
        if (!shouldTrackImpressionsCount()) {
            return;
        }
        mTaskExecutor.submit(
                mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
    }

    private void saveUniqueKeys() {
        if (!isNoneImpressionsMode()) {
            return;
        }
        mTaskExecutor.submit(
                mSplitTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()), null);
    }

    private boolean shouldTrackImpressionsCount() {
        return isOptimizedImpressionsMode() || isNoneImpressionsMode();
    }
}
