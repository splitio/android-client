package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutionInfo {
    final private String taskId;
    final private SplitTaskExecutionStatus status;

    public SplitTaskExecutionInfo(@NonNull String taskId, @NonNull SplitTaskExecutionStatus status) {
        this.taskId = checkNotNull(taskId);
        this.status = checkNotNull(status);
    }

    public SplitTaskExecutionStatus getStatus() {
        return status;
    }

    public String getTaskId() {
        return taskId;
    }
}
