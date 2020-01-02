package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

public interface SplitTaskExecutionListener {
    void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo);
}
