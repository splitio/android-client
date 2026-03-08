package io.split.android.client.submitter;

import io.split.android.client.service.executor.SplitTaskExecutionListener;

public interface RecorderSyncHelper<T extends InBytesSizable> extends SplitTaskExecutionListener {
    boolean pushAndCheckIfFlushNeeded(T entity);

    void addListener(SplitTaskExecutionListener listener);

    void removeListener(SplitTaskExecutionListener listener);
}
