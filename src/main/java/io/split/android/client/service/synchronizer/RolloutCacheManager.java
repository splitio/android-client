package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;

interface RolloutCacheManager {

    void validateCache(SplitTaskExecutionListener listener);
}
