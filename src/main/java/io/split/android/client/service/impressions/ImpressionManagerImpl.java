package io.split.android.client.service.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.service.synchronizer.RecorderSyncHelperImpl;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

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
    private final ImpressionManagerRetryTimerProvider mRetryTimerProvider;
    private AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);

    public ImpressionManagerImpl(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull ImpressionsTaskFactory splitTaskFactory,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull ImpressionsStorage impressionsStorage,
                                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                                 @NonNull ImpressionManagerConfig impressionManagerConfig) {
        this(
                taskExecutor,
                splitTaskFactory,
                telemetryRuntimeProducer,
                new ImpressionsCounter(),
                uniqueKeysTracker,
                impressionManagerConfig,
                new RecorderSyncHelperImpl<>(
                        SplitTaskType.IMPRESSIONS_RECORDER,
                        impressionsStorage,
                        impressionManagerConfig.getImpressionsQueueSize(),
                        impressionManagerConfig.getImpressionsChunkSize(),
                        taskExecutor),
                new ImpressionManagerRetryTimerProviderImpl(taskExecutor));
    }

    @VisibleForTesting
    public ImpressionManagerImpl(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull ImpressionsTaskFactory splitTaskFactory,
                                 @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                 @NonNull ImpressionsCounter impressionsCounter,
                                 @NonNull UniqueKeysTracker uniqueKeysTracker,
                                 @NonNull ImpressionManagerConfig impressionManagerConfig,
                                 @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                                 @NonNull ImpressionManagerRetryTimerProvider retryTimerProvider) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mImpressionManagerConfig = checkNotNull(impressionManagerConfig);

        mImpressionsObserver = new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        mImpressionsCounter = checkNotNull(impressionsCounter);
        mUniqueKeysTracker = checkNotNull(uniqueKeysTracker);

        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);

        mRetryTimerProvider = checkNotNull(retryTimerProvider);
    }

    @Override
    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }

    @Override
    public void pushImpression(Impression impression) {

        if (!isTrackingEnabled.get()) {
            Logger.v("Impression not tracked because tracking is disabled");
            return;
        }

        Long previousTime = mImpressionsObserver.testAndSet(impression);
        impression = impression.withPreviousTime(previousTime);
        if (shouldTrackImpressionsCount() && previousTimeIsValid(previousTime)) {
            mImpressionsCounter.inc(impression.split(), impression.time(), 1);
        }

        if (isNoneImpressionsMode()) {
            mUniqueKeysTracker.track(impression.key(), impression.split());

            if (mUniqueKeysTracker.size() >= ServiceConstants.MAX_UNIQUE_KEYS_IN_MEMORY) {
                saveUniqueKeys();
            }
        }

        KeyImpression keyImpression = KeyImpression.fromImpression(impression);
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
        flushImpressions();
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

    private void flushImpressions() {
        RetryBackoffCounterTimer retryTimer = mRetryTimerProvider.getImpressionsTimer();
        retryTimer.setTask(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        retryTimer.start();
    }

    private void flushImpressionsCount() {
        if (!shouldTrackImpressionsCount()) {
            return;
        }

        RetryBackoffCounterTimer retryTimer = mRetryTimerProvider.getImpressionsCountTimer();
        retryTimer.setTask(new SplitTaskSerialWrapper(
                mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()),
                mSplitTaskFactory.createImpressionsCountRecorderTask()));
        retryTimer.start();
    }

    private void flushUniqueKeys() {
        if (!isNoneImpressionsMode()) {
            return;
        }

        RetryBackoffCounterTimer retryTimer = mRetryTimerProvider.getUniqueKeysTimer();
        retryTimer.setTask(new SplitTaskSerialWrapper(
                mSplitTaskFactory.createSaveUniqueImpressionsTask(mUniqueKeysTracker.popAll()),
                mSplitTaskFactory.createUniqueImpressionsRecorderTask()));
        retryTimer.start();
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

    private static boolean previousTimeIsValid(Long previousTime) {
        return previousTime != null && previousTime != 0;
    }
}
