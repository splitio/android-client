package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;

public interface FeatureFlagsSynchronizer {

    void loadAndSynchronizeSplits();

    void loadSplitsFromCache();

    void synchronizeSplits(long since);

    void synchronizeSplits();

    void startFeatureFlagsPeriodicFetching();

    void stopFeatureFlagsPeriodicFetching();

    void stopSynchronization();

    void submitSplitLoadingTask(SplitTaskExecutionListener listener);
}
