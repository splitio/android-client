package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
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
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistryImpl;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener, MySegmentsSynchronizerRegistry, AttributesSynchronizerRegistry {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskExecutor mSplitsTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final ISplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;
    private final WorkManagerWrapper mWorkManagerWrapper;
    private final ImpressionManager mImpressionManager;

    private RecorderSyncHelper<Event> mEventsSyncHelper;

    private LoadLocalDataListener mLoadLocalSplitsListener;

    private String mSplitsFetcherTaskId;
    private String mEventsRecorderTaskId;
    private final RetryBackoffCounterTimer mSplitsSyncRetryTimer;
    private final RetryBackoffCounterTimer mSplitsUpdateRetryTimer;
    private final RetryBackoffCounterTimer mEventsRecorderUpdateRetryTimer;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final AttributesSynchronizerRegistryImpl mAttributesSynchronizerRegistry;
    private final MySegmentsSynchronizerRegistryImpl mMySegmentsSynchronizerRegistry;

    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitTaskExecutor splitSingleThreadTaskExecutor,
                            @NonNull SplitStorageContainer splitStorageContainer,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull ISplitEventsManager splitEventsManager,
                            @NonNull WorkManagerWrapper workManagerWrapper,
                            @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull AttributesSynchronizerRegistryImpl attributesSynchronizerRegistry,
                            @NonNull MySegmentsSynchronizerRegistryImpl mySegmentsSynchronizerRegistry,
                            @NonNull ImpressionManager impressionManager) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsTaskExecutor = splitSingleThreadTaskExecutor;
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
        mAttributesSynchronizerRegistry = attributesSynchronizerRegistry;
        mSplitsSyncRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);
        mSplitsUpdateRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);
        mEventsRecorderUpdateRetryTimer = retryBackoffCounterTimerFactory.createWithFixedInterval(
                mSplitsTaskExecutor,
                ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS,
                ServiceConstants.UNIQUE_KEYS_MAX_RETRY_ATTEMPTS);

        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mMySegmentsSynchronizerRegistry = checkNotNull(mySegmentsSynchronizerRegistry);
        mImpressionManager = checkNotNull(impressionManager);

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
        mMySegmentsSynchronizerRegistry.loadMySegmentsFromCache();
    }

    @Override
    public void loadAttributesFromCache() {
        mAttributesSynchronizerRegistry.loadAttributesFromCache();
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
        mMySegmentsSynchronizerRegistry.synchronizeMySegments();
    }

    @Override
    public void forceMySegmentsSync() {
        mMySegmentsSynchronizerRegistry.forceMySegmentsSync();
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
        mSplitsTaskExecutor.stopTask(mSplitsFetcherTaskId);
        mMySegmentsSynchronizerRegistry.stopPeriodicFetching();
    }

    @Override
    public void startPeriodicRecording() {
        scheduleEventsRecorderTask();
        mImpressionManager.startPeriodicRecording();
        Logger.i("Periodic recording tasks scheduled");
    }

    @Override
    public void stopPeriodicRecording() {
        mTaskExecutor.stopTask(mEventsRecorderTaskId);
        mImpressionManager.stopPeriodicRecording();
    }

    private void setupListeners() {
        mEventsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.EVENTS_RECORDER,
                mSplitsStorageContainer.getEventsStorage(),
                mSplitClientConfig.eventsQueueSize(),
                ServiceConstants.MAX_EVENTS_SIZE_BYTES,
                mTaskExecutor
        );

        mLoadLocalSplitsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    public void pause() {
        stopPeriodicRecording();
        mTaskExecutor.pause();
        mSplitsTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
        mSplitsTaskExecutor.resume();
        startPeriodicRecording();
    }

    @Override
    public void destroy() {
        mSplitsSyncRetryTimer.stop();
        mSplitsUpdateRetryTimer.stop();
        mMySegmentsSynchronizerRegistry.destroy();
        flush();
    }

    public void flush() {
        mEventsRecorderUpdateRetryTimer.setTask(mSplitTaskFactory.createEventsRecorderTask());
        mEventsRecorderUpdateRetryTimer.start();
        mImpressionManager.flush();
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
        mImpressionManager.pushImpression(impression);
    }

    @Override
    public void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizerRegistry.registerMySegmentsSynchronizer(userKey, mySegmentsSynchronizer);
    }

    @Override
    public void unregisterMySegmentsSynchronizer(String userKey) {
        mMySegmentsSynchronizerRegistry.unregisterMySegmentsSynchronizer(userKey);
    }

    @Override
    public void registerAttributesSynchronizer(String userKey, AttributesSynchronizer attributesSynchronizer) {
        mAttributesSynchronizerRegistry.registerAttributesSynchronizer(userKey, attributesSynchronizer);
    }

    @Override
    public void unregisterAttributesSynchronizer(String userKey) {
        mAttributesSynchronizerRegistry.unregisterAttributesSynchronizer(userKey);
    }

    private void scheduleSplitsFetcherTask() {
        mSplitsFetcherTaskId = mSplitsTaskExecutor.schedule(
                mSplitTaskFactory.createSplitsSyncTask(false),
                mSplitClientConfig.featuresRefreshRate(),
                mSplitClientConfig.featuresRefreshRate(),
                null);
    }

    private void scheduleMySegmentsFetcherTask() {
        mMySegmentsSynchronizerRegistry.scheduleSegmentsSyncTask();
    }

    private void scheduleEventsRecorderTask() {
        mEventsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createEventsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.eventFlushInterval(), mEventsSyncHelper);
    }

    private void submitSplitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
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
                mMySegmentsSynchronizerRegistry.submitMySegmentsLoadingTask();
                break;
        }
    }
}
