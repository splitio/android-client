package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.storage.common.InBytesSizable;

public interface RecorderSyncHelper<T extends InBytesSizable> extends SplitTaskExecutionListener {
    boolean pushAndCheckIfFlushNeeded(T entity);

    void addListener(SplitTaskExecutionListener listener);

    void removeListener(SplitTaskExecutionListener listener);
}
