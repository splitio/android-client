package io.split.android.client.service;


import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager {

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
        SplitsSyncTask splitsSyncTask = new SplitsSyncTask(
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
}
