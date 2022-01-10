
package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionUtils;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySyncTaskExecutionListener;
import io.split.android.client.telemetry.TelemetrySyncTaskExecutionListenerFactory;
import io.split.android.client.telemetry.TelemetrySyncTaskExecutionListenerFactoryImpl;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;
    private final WorkManagerWrapper mWorkManagerWrapper;

    private RecorderSyncHelper<Event> mEventsSyncHelper;
    private RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final TelemetrySyncTaskExecutionListener mEventsTelemetryTaskListener;
    private final TelemetrySyncTaskExecutionListener mImpressionsTelemetryTaskListener;
    private final TelemetrySyncTaskExecutionListener mImpressionsCountTelemetryTaskListener;
    private final TelemetrySyncTaskExecutionListener mSplitsTelemetryTaskListener;
    private final TelemetrySyncTaskExecutionListener mMySegmentsTelemetryTaskListener;

    private LoadLocalDataListener mLoadLocalSplitsListener;
    private LoadLocalDataListener mLoadLocalMySegmentsListener;
    private LoadLocalDataListener mLoadLocalAttributesListener;

    private String mSplitsFetcherTaskId;
    private String mMySegmentsFetcherTaskId;
    private String mEventsRecorderTaskId;
    private String mImpressionsRecorderTaskId;
    private String mImpressionsRecorderCountTaskId;
    private final RetryBackoffCounterTimer mSplitsSyncRetryTimer;
    private final RetryBackoffCounterTimer mSplitsUpdateRetryTimer;
    private final RetryBackoffCounterTimer mMySegmentsSyncRetryTimer;
    private final ImpressionsObserver mImpressionsObserver;
    private final ImpressionsCounter mImpressionsCounter;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitStorageContainer splitStorageContainer,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull SplitEventsManager splitEventsManager,
                            @NonNull WorkManagerWrapper workManagerWrapper,
                            @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull TelemetrySyncTaskExecutionListenerFactory telemetrySyncTaskExecutionListenerFactory) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
        mSplitsSyncRetryTimer = retryBackoffCounterTimerFactory.create(taskExecutor, 1);
        mSplitsUpdateRetryTimer = retryBackoffCounterTimerFactory.create(taskExecutor, 1);

        mMySegmentsSyncRetryTimer = retryBackoffCounterTimerFactory.create(taskExecutor, 1);

        mImpressionsObserver = new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        mImpressionsCounter = new ImpressionsCounter();

        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        mEventsTelemetryTaskListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.EVENTS_RECORDER, OperationType.EVENTS);
        mImpressionsTelemetryTaskListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.IMPRESSIONS_RECORDER, OperationType.IMPRESSIONS);
        mImpressionsCountTelemetryTaskListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.IMPRESSIONS_COUNT_RECORDER, OperationType.IMPRESSIONS_COUNT);
        mSplitsTelemetryTaskListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.SPLITS_SYNC, OperationType.SPLITS);
        mMySegmentsTelemetryTaskListener = telemetrySyncTaskExecutionListenerFactory.create(SplitTaskType.MY_SEGMENTS_SYNC, OperationType.MY_SEGMENT);

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
        submitMySegmentsLoadingTask(mLoadLocalMySegmentsListener);
    }

    @Override
    public void loadAttributesFromCache() {
        submitAttributesLoadingTask(mLoadLocalAttributesListener, mSplitClientConfig.persistentAttributesEnabled());
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
        mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(false), null);
        mMySegmentsSyncRetryTimer.start();
    }

    @Override
    public void forceMySegmentsSync() {
        mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(true), null);
        mMySegmentsSyncRetryTimer.start();
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
        mTaskExecutor.stopTask(mMySegmentsFetcherTaskId);
    }

    @Override
    public void startPeriodicRecording() {
        scheduleEventsRecorderTask();
        scheduleImpressionsRecorderTask();
        scheduleImpressionsCountRecorderTask();
        Logger.i("Peridic recording tasks scheduled");
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
                mTaskExecutor,
                mEventsTelemetryTaskListener);

        mImpressionsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mSplitsStorageContainer.getImpressionsStorage(),
                mSplitClientConfig.impressionsQueueSize(),
                mSplitClientConfig.impressionsChunkSize(),
                mTaskExecutor,
                mImpressionsTelemetryTaskListener);

        mLoadLocalSplitsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        mLoadLocalMySegmentsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);

        mLoadLocalAttributesListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
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
        mMySegmentsSyncRetryTimer.stop();
        mSplitsUpdateRetryTimer.stop();
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
        }
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
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createImpressionsCountRecorderTask(),
                mImpressionsCountTelemetryTaskListener));
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
                mSplitsTelemetryTaskListener);
    }

    private void scheduleMySegmentsFetcherTask() {
        mMySegmentsFetcherTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createMySegmentsSyncTask(false),
                mSplitClientConfig.segmentsRefreshRate(),
                mSplitClientConfig.segmentsRefreshRate(),
                mMySegmentsTelemetryTaskListener);
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
                mImpressionsCountTelemetryTaskListener);
    }

    private void submitSplitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
    }

    private void submitMySegmentsLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadMySegmentsTask(),
                listener);
    }

    private void submitAttributesLoadingTask(SplitTaskExecutionListener listener, boolean persistentAttributesEnabled) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadAttributesTask(persistentAttributesEnabled),
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
                submitMySegmentsLoadingTask(null);
                break;
        }
    }
}
