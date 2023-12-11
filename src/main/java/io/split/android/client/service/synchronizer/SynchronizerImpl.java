package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistryImpl;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.common.StoragePusher;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener, MySegmentsSynchronizerRegistry, AttributesSynchronizerRegistry {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskExecutor mSingleThreadTaskExecutor;
    private final StoragePusher<Event> mEventsStorage;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitTaskFactory mSplitTaskFactory;
    private final ImpressionManager mImpressionManager;
    private final FeatureFlagsSynchronizer mFeatureFlagsSynchronizer;

    private RecorderSyncHelper<Event> mEventsSyncHelper;

    private String mEventsRecorderTaskId;
    private final RetryBackoffCounterTimer mEventsRecorderUpdateRetryTimer;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final AttributesSynchronizerRegistryImpl mAttributesSynchronizerRegistry;
    private final MySegmentsSynchronizerRegistryImpl mMySegmentsSynchronizerRegistry;

    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitTaskExecutor splitSingleThreadTaskExecutor,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull WorkManagerWrapper workManagerWrapper,
                            @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull AttributesSynchronizerRegistryImpl attributesSynchronizerRegistry,
                            @NonNull MySegmentsSynchronizerRegistryImpl mySegmentsSynchronizerRegistry,
                            @NonNull ImpressionManager impressionManager,
                            @NonNull StoragePusher<Event> eventsStorage,
                            @NonNull ISplitEventsManager eventsManagerCoordinator,
                            @Nullable PushManagerEventBroadcaster pushManagerEventBroadcaster,
                            @NonNull String splitsFilterQueryStringFromConfig) {
        this(splitClientConfig,
                taskExecutor,
                splitSingleThreadTaskExecutor,
                splitTaskFactory,
                workManagerWrapper,
                retryBackoffCounterTimerFactory,
                telemetryRuntimeProducer,
                attributesSynchronizerRegistry,
                mySegmentsSynchronizerRegistry,
                impressionManager,
                new FeatureFlagsSynchronizerImpl(splitClientConfig, taskExecutor,
                        splitSingleThreadTaskExecutor,
                        splitTaskFactory,
                        eventsManagerCoordinator,
                        retryBackoffCounterTimerFactory,
                        pushManagerEventBroadcaster,
                        splitsFilterQueryStringFromConfig),
                eventsStorage);
    }

    @VisibleForTesting
    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitTaskExecutor splitSingleThreadTaskExecutor,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull WorkManagerWrapper workManagerWrapper,
                            @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull AttributesSynchronizerRegistryImpl attributesSynchronizerRegistry,
                            @NonNull MySegmentsSynchronizerRegistryImpl mySegmentsSynchronizerRegistry,
                            @NonNull ImpressionManager impressionManager,
                            @NonNull FeatureFlagsSynchronizer featureFlagsSynchronizer,
                            @NonNull StoragePusher<Event> eventsStorage) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSingleThreadTaskExecutor = checkNotNull(splitSingleThreadTaskExecutor);
        mEventsStorage = checkNotNull(eventsStorage);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mFeatureFlagsSynchronizer = checkNotNull(featureFlagsSynchronizer);
        mAttributesSynchronizerRegistry = attributesSynchronizerRegistry;
        mEventsRecorderUpdateRetryTimer = retryBackoffCounterTimerFactory.createWithFixedInterval(
                mSingleThreadTaskExecutor,
                ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS,
                ServiceConstants.UNIQUE_KEYS_MAX_RETRY_ATTEMPTS);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mMySegmentsSynchronizerRegistry = checkNotNull(mySegmentsSynchronizerRegistry);
        mImpressionManager = checkNotNull(impressionManager);

        setupListeners();

        if (mSplitClientConfig.synchronizeInBackground()) {
            workManagerWrapper.setFetcherExecutionListener(this);
            workManagerWrapper.scheduleWork();
        } else {
            workManagerWrapper.removeWork();
        }
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
                mEventsStorage,
                mSplitClientConfig.eventsQueueSize(),
                ServiceConstants.MAX_EVENTS_SIZE_BYTES,
                mTaskExecutor
        );

    }

    public void pause() {
        stopPeriodicRecording();
        stopPeriodicFetching();

        mTaskExecutor.pause();
        mSingleThreadTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
        mSingleThreadTaskExecutor.resume();
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
