package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;

public interface FeatureFlagsSynchronizer {

    void loadAndSynchronize();

    void synchronize(Long since, Long rbsSince);

    void synchronize();

    void startPeriodicFetching();

    void stopPeriodicFetching();

    void stopSynchronization();

    void submitLoadingTask(SplitTaskExecutionListener listener);
}
