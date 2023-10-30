package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;

public interface FeatureFlagsSynchronizer {

    void loadAndSynchronize();

    void synchronize(long since);

    void synchronize();

    void startPeriodicFetching();

    void stopPeriodicFetching();

    void stopSynchronization();

    void submitLoadingTask(SplitTaskExecutionListener listener);
}
