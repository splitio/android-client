package io.split.android.client.service;


import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager, SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;

    public static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    private int pushedEventCount = 0;
    private long totalEventsSizeInBytes = 0;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull SplitTaskExecutor taskExecutor,
                           @NonNull SplitApiFacade splitApiFacade,
                           @NonNull SplitStorageContainer splitStorageContainer) {

        checkNotNull(taskExecutor);
        checkNotNull(splitApiFacade);
        checkNotNull(splitStorageContainer);
        checkNotNull(splitClientConfig);

        mTaskExecutor = taskExecutor;
        mSplitApiFacade = splitApiFacade;
        mSplitsStorageContainer = splitStorageContainer;
        mSplitClientConfig = splitClientConfig;
    }

    @Override
    public void start() {
        scheduleTasks();
    }

    @Override
    public void pause() {
        mTaskExecutor.pause();
    }

    @Override
    public void resume() {
        mTaskExecutor.resume();
    }

    @Override
    public void stop() {
        mTaskExecutor.stop();
    }

    private void scheduleTasks() {
        scheduleSplitsFetcherTask();
        scheduleMySegmentsFetcherTask();
        scheduleEventsRecorderTask();
    }

    private void scheduleSplitsFetcherTask() {
        SplitTask splitsSyncTask = new SplitsSyncTask(
                mSplitApiFacade.getSplitFetcher(),
                mSplitsStorageContainer.getSplitsStorage(),
                new SplitChangeProcessor());
        mTaskExecutor.schedule(splitsSyncTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }

    @Override
    public void pushEvent(Event event) {
        PersistentEventsStorage eventsStorage = mSplitsStorageContainer.getEventsStorage();
        eventsStorage.push(event);
        pushedEventCount++;
        totalEventsSizeInBytes += event.getSizeInBytes();
        if (pushedEventCount > mSplitClientConfig.eventsQueueSize() ||
                totalEventsSizeInBytes >= MAX_EVENTS_SIZE_BYTES) {
            // TODO: schedule event recording task
        }
      }
    private void scheduleMySegmentsFetcherTask() {
        SplitTask mySegmentsSyncTask = new MySegmentsSyncTask(
                mSplitApiFacade.getMySegmentsFetcher(),
                mSplitsStorageContainer.getMySegmentsStorage());
        mTaskExecutor.schedule(mySegmentsSyncTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }

    private void scheduleEventsRecorderTask() {

        SplitTask eventsRecorderTask = new EventsRecorderTask(
                "taskId",
                this,
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()));
        mTaskExecutor.schedule(eventsRecorderTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
    }
}
