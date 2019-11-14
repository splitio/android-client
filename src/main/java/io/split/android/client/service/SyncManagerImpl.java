package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.storage.SplitStorageProvider;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitFetcherProvider mSplitFetcherProvider;
    private final SplitStorageProvider mSplitsStorageProvider;
    private final SplitClientConfig mSplitClientConfig;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull SplitTaskExecutor taskExecutor,
                           @NonNull SplitFetcherProvider splitFetcherProvider,
                           @NonNull SplitStorageProvider splitStorageProvider) {

        checkNotNull(taskExecutor);
        checkNotNull(splitFetcherProvider);
        checkNotNull(splitStorageProvider);
        checkNotNull(splitClientConfig);

        mTaskExecutor = taskExecutor;
        mSplitFetcherProvider = splitFetcherProvider;
        mSplitsStorageProvider = splitStorageProvider;
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
                mSplitFetcherProvider.getSplitFetcher(),
                mSplitsStorageProvider.getSplitStorage());
        mTaskExecutor.schedule(splitsSyncTask, 0L, mSplitClientConfig.featuresRefreshRate());
    }
}
