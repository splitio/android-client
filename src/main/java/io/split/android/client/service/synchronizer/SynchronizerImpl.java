package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionUtils;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegister;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegister;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;

public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener, MySegmentsSynchronizerRegister, AttributesSynchronizerRegister {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final ISplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;
    private final WorkManagerWrapper mWorkManagerWrapper;

    private RecorderSyncHelper<Event> mEventsSyncHelper;
    private RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;

    private LoadLocalDataListener mLoadLocalSplitsListener;

    private String mSplitsFetcherTaskId;
    private String mEventsRecorderTaskId;
    private String mImpressionsRecorderTaskId;
    private String mImpressionsRecorderCountTaskId;
    private final RetryBackoffCounterTimer mSplitsSyncRetryTimer;
    private final RetryBackoffCounterTimer mSplitsUpdateRetryTimer;
    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final ConcurrentMap<String, MySegmentsSynchronizer> mMySegmentsSynchronizers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AttributesSynchronizer> mAttributesSynchronizers = new ConcurrentHashMap<>();

    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitStorageContainer splitStorageContainer,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull ISplitEventsManager splitEventsManager,
                            @NonNull WorkManagerWrapper workManagerWrapper,
                            @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
        mSplitsSyncRetryTimer = retryBackoffCounterTimerFactory.create(taskExecutor, 1);
        mSplitsUpdateRetryTimer = retryBackoffCounterTimerFactory.create(taskExecutor, 1);

        mImpressionsObserver = new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        mImpressionsCounter = new ImpressionsCounter();

        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        setupListeners();
        mSplitsSyncRetryTimer.setTask(mSplitTaskFactory.createSplitsSyncTask(true), null);

        if (mSplitClientConfig.synchronizeInBackground()) {
            mWorkManagerWrapper.setFetcherExecutionListener(this);
            mWorkManagerWrapper.scheduleWork();
        } else {
            mWorkManagerWrapper.removeWork();
        }
    }

    @Override
    public void loadSplitsFromCache() {
        submitSplitLoadingTask(mLoadLocalSplitsListener);
    }

    @Override
    public void loadMySegmentsFromCache() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.loadMySegmentsFromCache();
        }
    }

    @Override
    public void loadAttributesFromCache() {
        for (AttributesSynchronizer attributesSynchronizer : mAttributesSynchronizers.values()) {
            attributesSynchronizer.loadAttributesFromCache();
        }
    }

    @Override
    public void loadAndSynchronizeSplits() {
        List<SplitTaskBatchItem> enqueued = new ArrayList<>();
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createFilterSplitsInCacheTask(), null));
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createLoadSplitsTask(), mLoadLocalSplitsListener));
        enqueued.add(new SplitTaskBatchItem(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                synchronizeSplits();
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        }, null));
        mTaskExecutor.executeSerially(enqueued);
    }

    @Override
    public void synchronizeSplits(long since) {
        mSplitsUpdateRetryTimer.setTask(mSplitTaskFactory.createSplitsUpdateTask(since), null);
        mSplitsUpdateRetryTimer.start();
    }

    @Override
    public void synchronizeSplits() {
        mSplitsSyncRetryTimer.start();
    }

    @Override
    public void synchronizeMySegments() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.synchronizeMySegments();
        }
    }

    @Override
    public void forceMySegmentsSync() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.forceMySegmentsSync();
        }
    }

    @Override
    synchronized public void startPeriodicFetching() {
        scheduleSplitsFetcherTask();
        scheduleMySegmentsFetcherTask();
        mTelemetryRuntimeProducer.recordStreamingEvents(new SyncModeUpdateStreamingEvent(SyncModeUpdateStreamingEvent.Mode.POLLING, System.currentTimeMillis()));
        Logger.i("Periodic fetcher tasks scheduled");
    }

    @Override
    synchronized public void stopPeriodicFetching() {
        mTaskExecutor.stopTask(mSplitsFetcherTaskId);
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.stopPeriodicFetching();
        }
    }

    @Override
    public void startPeriodicRecording() {
        scheduleEventsRecorderTask();
        scheduleImpressionsRecorderTask();
        scheduleImpressionsCountRecorderTask();
        Logger.i("Periodic recording tasks scheduled");
    }

    @Override
    public void stopPeriodicRecording() {
        saveImpressionsCount();
        mTaskExecutor.stopTask(mEventsRecorderTaskId);
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        mTaskExecutor.stopTask(mImpressionsRecorderCountTaskId);
    }

    private void setupListeners() {
        mEventsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.EVENTS_RECORDER,
                mSplitsStorageContainer.getEventsStorage(),
                mSplitClientConfig.eventsQueueSize(),
                ServiceConstants.MAX_EVENTS_SIZE_BYTES,
                mTaskExecutor
        );

        mImpressionsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mSplitsStorageContainer.getImpressionsStorage(),
                mSplitClientConfig.impressionsQueueSize(),
                mSplitClientConfig.impressionsChunkSize(),
                mTaskExecutor
        );

        mLoadLocalSplitsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    public void pause() {
        stopPeriodicRecording();
        mTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
        startPeriodicRecording();
    }

    @Override
    public void destroy() {
        mSplitsSyncRetryTimer.stop();
        mSplitsUpdateRetryTimer.stop();
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.destroy();
        }
        flush();
    }

    public void flush() {
        mTaskExecutor.submit(mSplitTaskFactory.createEventsRecorderTask(),
                mEventsSyncHelper);
        mTaskExecutor.submit(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        flushImpressionsCount();
    }

    @Override
    public void pushEvent(Event event) {
        if (mEventsSyncHelper.pushAndCheckIfFlushNeeded(event)) {
            mTaskExecutor.submit(
                    mSplitTaskFactory.createEventsRecorderTask(),
                    mEventsSyncHelper);
        }
        mTelemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1);
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
    public void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizers.put(userKey, mySegmentsSynchronizer);
    }

    @Override
    public void unregisterMySegmentsSynchronizer(String userKey) {
        mMySegmentsSynchronizers.remove(userKey);
    }

    @Override
    public void registerAttributesSynchronizer(String userKey, AttributesSynchronizer attributesSynchronizer) {
        mAttributesSynchronizers.put(userKey, attributesSynchronizer);
    }

    @Override
    public void unregisterAttributesSynchronizer(String userKey) {
        mAttributesSynchronizers.remove(userKey);
    }

    private void saveImpressionsCount() {
        if (!isOptimizedImpressionsMode()) {
            return;
        }
        mTaskExecutor.submit(
                mSplitTaskFactory.createSaveImpressionsCountTask(mImpressionsCounter.popAll()), null);
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

    private boolean isOptimizedImpressionsMode() {
        return ImpressionsMode.OPTIMIZED.equals(mSplitClientConfig.impressionsMode());
    }

    private void scheduleSplitsFetcherTask() {
        mSplitsFetcherTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createSplitsSyncTask(false),
                mSplitClientConfig.featuresRefreshRate(),
                mSplitClientConfig.featuresRefreshRate(),
                null);
    }

    private void scheduleMySegmentsFetcherTask() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.scheduleSegmentsSyncTask();
        }
    }

    private void scheduleEventsRecorderTask() {
        mEventsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createEventsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.eventFlushInterval(), mEventsSyncHelper);
    }

    private void scheduleImpressionsRecorderTask() {
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.impressionsRefreshRate(), mImpressionsSyncHelper);
    }

    private void scheduleImpressionsCountRecorderTask() {
        if (!isOptimizedImpressionsMode()) {
            return;
        }
        mImpressionsRecorderCountTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createImpressionsCountRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.impressionsCounterRefreshRate(),
                null);
    }

    private void submitSplitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
    }

    private boolean shouldPushImpression(KeyImpression impression) {
        return impression.previousTime == null ||
                ImpressionUtils.truncateTimeframe(impression.previousTime) != ImpressionUtils.truncateTimeframe(impression.time);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        switch (taskInfo.getTaskType()) {
            case SPLITS_SYNC:
                Logger.d("Loading split definitions updated in background");
                submitSplitLoadingTask(null);
                break;
            case MY_SEGMENTS_SYNC:
                Logger.d("Loading my segments updated in background");
                for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
                    mySegmentsSynchronizer.submitMySegmentsLoadingTask();
                }
                break;
        }
    }
}
