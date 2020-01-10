package io.split.android.client.service;


import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.*;

public class SyncManagerImpl implements SyncManager, SplitTaskExecutionListener {

    private static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    private final SplitTaskExecutor mTaskExecutor;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;

    private AtomicInteger mPushedEventCount;
    private AtomicLong mTotalEventsSizeInBytes;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull SplitTaskExecutor taskExecutor,
                           @NonNull SplitApiFacade splitApiFacade,
                           @NonNull SplitStorageContainer splitStorageContainer) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mPushedEventCount = new AtomicInteger(0);
        mTotalEventsSizeInBytes = new AtomicLong(0);
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

    private void scheduleMySegmentsFetcherTask() {
        SplitTask mySegmentsSyncTask = new MySegmentsSyncTask(
                mSplitApiFacade.getMySegmentsFetcher(),
                mSplitsStorageContainer.getMySegmentsStorage());
        mTaskExecutor.schedule(mySegmentsSyncTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }

    private void scheduleEventsRecorderTask() {
        mTaskExecutor.schedule(createEventsSyncTask(), 0L, mSplitClientConfig.featuresRefreshRate());
    }

    @Override
    public void pushEvent(Event event) {
        PersistentEventsStorage eventsStorage = mSplitsStorageContainer.getEventsStorage();
        eventsStorage.push(event);
        int pushedEventCount = mPushedEventCount.addAndGet(1);
        long totalEventsSizeInBytes = mTotalEventsSizeInBytes.addAndGet(event.getSizeInBytes());
        if (pushedEventCount > mSplitClientConfig.eventsQueueSize() ||
                totalEventsSizeInBytes >= MAX_EVENTS_SIZE_BYTES) {
            mPushedEventCount.set(0);
            mTotalEventsSizeInBytes.set(0);
            mTaskExecutor.submit(createEventsSyncTask());
        }
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        switch (taskInfo.getTaskType()) {
            case EVENTS_RECORDER:
                updateEventsTaskStatus(taskInfo);
                break;
        }
    }

    private SplitTask createEventsSyncTask() {
        return new EventsRecorderTask(
                EVENTS_RECORDER,
                this,
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()));
    }

    private void updateEventsTaskStatus(SplitTaskExecutionInfo executionInfo) {
        if(executionInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            mPushedEventCount.addAndGet(executionInfo.getNonSentRecords());
            mTotalEventsSizeInBytes.addAndGet(executionInfo.getNonSentBytes());
        }
    }
}
