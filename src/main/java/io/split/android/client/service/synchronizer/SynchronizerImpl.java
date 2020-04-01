package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.ParameterizableSplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class SynchronizerImpl implements Synchronizer, SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;

    private RecorderSyncHelper<Event> mEventsSyncHelper;
    private RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;

    private FetcherSyncListener mSplitsSyncTaskListener;
    private FetcherSyncListener mMySegmentsSyncTaskListener;

    private LoadLocalDataListener mLoadLocalSplitsListener;
    private LoadLocalDataListener mLoadLocalMySegmentsListener;

    private WorkManagerWrapper mWorkManagerWrapper;

    private String mSplitsFetcherTaskId;
    private String mMySegmentsFetcherTaskId;
    private String mEventsRecorderTaskId;
    private String mImpressionsRecorderTaskId;

    public SynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                            @NonNull SplitTaskExecutor taskExecutor,
                            @NonNull SplitStorageContainer splitStorageContainer,
                            @NonNull SplitTaskFactory splitTaskFactory,
                            @NonNull SplitEventsManager splitEventsManager,
                            @NonNull WorkManagerWrapper workManagerWrapper) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);

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
        submitSplitLoadingTask(mLoadLocalSplitsListener);
    }

    @Override
    public void loadMySegmentsFromCache() {
        submitMySegmentsLoadingTask(mLoadLocalMySegmentsListener);
    }

    @Override
    public void synchronizeSplits(long since) {
        ParameterizableSplitTask<Long> splitsUpdateTask
                = mSplitTaskFactory.createSplitsUpdateTask();
        splitsUpdateTask.setParam(since);
        mTaskExecutor.submit(splitsUpdateTask, mSplitsSyncTaskListener);
    }

    @Override
    public void synchronizeSplits() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createSplitsSyncTask(),
                mSplitsSyncTaskListener);
    }

    @Override
    public void syncronizeMySegments() {
        submitMySegmentsLoadingTask(mMySegmentsSyncTaskListener);
    }

    @Override
    public void startPeriodicFetching() {
        scheduleSplitsFetcherTask();
        scheduleMySegmentsFetcherTask();
        Logger.i("Synchronization tasks scheduled");
    }

    @Override
    public void stopPeriodicFetching() {
        List<String> taskIds = new ArrayList<>();
        taskIds.add(mSplitsFetcherTaskId);
        taskIds.add(mMySegmentsFetcherTaskId);
        mTaskExecutor.stopTasks(taskIds);
    }

    @Override
    public void startPeriodicRecording() {
        scheduleEventsRecorderTask();
        scheduleImpressionsRecorderTask();
        Logger.i("Synchronization tasks scheduled");
    }

    @Override
    public void stopPeriodicRecording() {
        List<String> taskIds = new ArrayList<>();
        taskIds.add(mEventsRecorderTaskId);
        taskIds.add(mImpressionsRecorderTaskId);
        mTaskExecutor.stopTasks(taskIds);
    }

    private void setupListeners() {
        mEventsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.EVENTS_RECORDER,
                mSplitsStorageContainer.getEventsStorage(),
                mSplitClientConfig.eventsQueueSize(),
                ServiceConstants.MAX_EVENTS_SIZE_BYTES);

        mImpressionsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mSplitsStorageContainer.getImpressionsStorage(),
                mSplitClientConfig.impressionsQueueSize(),
                mSplitClientConfig.impressionsChunkSize());

        mSplitsSyncTaskListener = new FetcherSyncListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_ARE_READY);

        mMySegmentsSyncTaskListener = new FetcherSyncListener(
                mSplitEventsManager, SplitInternalEvent.MYSEGEMENTS_ARE_READY);

        mLoadLocalSplitsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        mLoadLocalMySegmentsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.MYSEGMENTS_LOADED_FROM_STORAGE);
    }

    public void pause() {
        mTaskExecutor.pause();
    }


    public void resume() {
        mTaskExecutor.resume();
    }

    @Override
    public void destroy() {
        flush();
        mTaskExecutor.stop();
    }

    public void flush() {
        mTaskExecutor.submit(mSplitTaskFactory.createEventsRecorderTask(),
                mEventsSyncHelper);
        mTaskExecutor.submit(
                mSplitTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
    }

    @Override
    public void pushEvent(Event event) {
        if (mEventsSyncHelper.pushAndCheckIfFlushNeeded(event)) {
            mTaskExecutor.submit(
                    mSplitTaskFactory.createEventsRecorderTask(),
                    mEventsSyncHelper);
        }
    }

    @Override
    public void pushImpression(Impression impression) {
        if (mImpressionsSyncHelper.pushAndCheckIfFlushNeeded(new KeyImpression(impression))) {
            mTaskExecutor.submit(
                    mSplitTaskFactory.createImpressionsRecorderTask(),
                    mImpressionsSyncHelper);
        }
    }

    private void scheduleSplitsFetcherTask() {
        mSplitsFetcherTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createSplitsSyncTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.featuresRefreshRate(),
                mSplitsSyncTaskListener);
    }

    private void scheduleMySegmentsFetcherTask() {
        mMySegmentsFetcherTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createMySegmentsSyncTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mSplitClientConfig.segmentsRefreshRate(), mMySegmentsSyncTaskListener);
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

    private void submitSplitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
    }

    private void submitMySegmentsLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadMySegmentsTask(),
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
                submitMySegmentsLoadingTask(null);
                break;
        }
    }
}
