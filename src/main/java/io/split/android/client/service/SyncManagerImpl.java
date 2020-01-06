package io.split.android.client.service;


import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.storage.SplitStorageContainer;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageProvider;
    private final SplitClientConfig mSplitClientConfig;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull SplitTaskExecutor taskExecutor,
                           @NonNull SplitApiFacade splitApiFacade,
                           @NonNull SplitStorageContainer splitStorageContainer) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageProvider = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
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
                mSplitsStorageProvider.getSplitStorage(),
                new SplitChangeProcessor());
        mTaskExecutor.schedule(splitsSyncTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }
}
