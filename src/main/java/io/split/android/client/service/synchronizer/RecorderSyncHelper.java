package io.split.android.client.service.synchronizer;

import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.storage.InBytesSizable;

@VisibleForTesting()
public interface RecorderSyncHelper<T extends InBytesSizable> extends SplitTaskExecutionListener {
    boolean pushAndCheckIfFlushNeeded(T entity);
}
