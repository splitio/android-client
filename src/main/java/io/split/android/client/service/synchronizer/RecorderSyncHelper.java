package io.split.android.client.service.synchronizer;

import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.storage.InBytesSizable;

interface RecorderSyncHelper<T extends InBytesSizable> extends SplitTaskExecutionListener {
    boolean pushAndCheckIfFlushNeeded(T entity);
}
