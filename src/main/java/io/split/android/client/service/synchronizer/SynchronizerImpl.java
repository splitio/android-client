package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
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
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener, MySegmentsSynchronizerRegistry, AttributesSynchronizerRegistry {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskExecutor mSplitsTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitTaskFactory mSplitTaskFactory;
    private final WorkManagerWrapper mWorkManagerWrapper;
    private final ImpressionManager mImpressionManager;

    private RecorderSyncHelper<Event> mEventsSyncHelper;

    private String mEventsRecorderTaskId;
    private final RetryBackoffCounterTimer mEventsRecorderUpdateRetryTimer;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final AttributesSynchronizerRegistryImpl mAttributesSynchronizerRegistry;
    private final MySegmentsSynchronizerRegistryImpl mMySegmentsSynchronizerRegistry;
    private final FeatureFlagsSynchronizer mFeatureFlagsSynchronizer;

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
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
        mAttributesSynchronizerRegistry = attributesSynchronizerRegistry;
        mEventsRecorderUpdateRetryTimer = retryBackoffCounterTimerFactory.createWithFixedInterval(
                mSplitsTaskExecutor,
                ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS,
                ServiceConstants.UNIQUE_KEYS_MAX_RETRY_ATTEMPTS);
        mFeatureFlagsSynchronizer = new FeatureFlagsSynchronizerImpl(splitClientConfig,
                taskExecutor,
                splitSingleThreadTaskExecutor,
                splitTaskFactory,
                splitEventsManager,
                retryBackoffCounterTimerFactory);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mMySegmentsSynchronizerRegistry = checkNotNull(mySegmentsSynchronizerRegistry);
        mImpressionManager = checkNotNull(impressionManager);

        setupListeners();

        if (mSplitClientConfig.synchronizeInBackground()) {
            mWorkManagerWrapper.setFetcherExecutionListener(this);
            mWorkManagerWrapper.scheduleWork();
        } else {
            mWorkManagerWrapper.removeWork();
        }
    }

    @Override
    public void loadSplitsFromCache() {
        mFeatureFlagsSynchronizer.loadFromCache();
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
        mFeatureFlagsSynchronizer.loadAndSynchronize();
    }

    @Override
    public void synchronizeSplits(long since) {
        mFeatureFlagsSynchronizer.synchronize(since);
    }

    @Override
    public void synchronizeSplits() {
        mFeatureFlagsSynchronizer.synchronize();
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
        mFeatureFlagsSynchronizer.startPeriodicFetching();
        scheduleMySegmentsFetcherTask();
        mTelemetryRuntimeProducer.recordStreamingEvents(new SyncModeUpdateStreamingEvent(SyncModeUpdateStreamingEvent.Mode.POLLING, System.currentTimeMillis()));
        Logger.i("Periodic fetcher tasks scheduled");
    }

    @Override
    synchronized public void stopPeriodicFetching() {
        mFeatureFlagsSynchronizer.stopPeriodicFetching();
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
        mEventsRecorderTaskId = null;
    }

    private void setupListeners() {
        mEventsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.EVENTS_RECORDER,
                mSplitsStorageContainer.getEventsStorage(),
                mSplitClientConfig.eventsQueueSize(),
                ServiceConstants.MAX_EVENTS_SIZE_BYTES,
                mTaskExecutor
        );

    }

    public void pause() {
        stopPeriodicRecording();
        mTaskExecutor.pause();
        mSplitsTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
        mSplitsTaskExecutor.resume();
        if (mSplitClientConfig.userConsent() == UserConsent.GRANTED) {
            startPeriodicRecording();
        }
    }

    @Override
    public void destroy() {
        mFeatureFlagsSynchronizer.stopSynchronization();
        mMySegmentsSynchronizerRegistry.destroy();
        flush();
    }

    public void flush() {
        if (mSplitClientConfig.userConsent() == UserConsent.GRANTED) {
            mEventsRecorderUpdateRetryTimer.setTask(mSplitTaskFactory.createEventsRecorderTask());
            mEventsRecorderUpdateRetryTimer.start();
            mImpressionManager.flush();
        }
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

    private void scheduleMySegmentsFetcherTask() {
        mMySegmentsSynchronizerRegistry.scheduleSegmentsSyncTask();
    }

    private void scheduleEventsRecorderTask() {
        mEventsRecorderTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createEventsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.eventFlushInterval(), mEventsSyncHelper);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        switch (taskInfo.getTaskType()) {
            case SPLITS_SYNC:
                mFeatureFlagsSynchronizer.submitLoadingTask(null);
                break;
            case MY_SEGMENTS_SYNC:
                Logger.d("Loading my segments updated in background");
                mMySegmentsSynchronizerRegistry.submitMySegmentsLoadingTask();
                break;
        }
    }
}
