package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.spi.StreamingScheduler;

/**
 * Adapter that implements StreamingScheduler using SplitTaskExecutor.
 */
public class SplitTaskExecutorStreamingScheduler implements StreamingScheduler {

    private final SplitTaskExecutor mTaskExecutor;

    public SplitTaskExecutorStreamingScheduler(@NonNull SplitTaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    @NonNull
    @Override
    public String schedule(@NonNull Runnable task, long delaySeconds, @Nullable TaskExecutionListener listener) {
        return mTaskExecutor.schedule(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                try {
                    task.run();
                    return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
                } catch (Exception e) {
                    return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK, SplitTaskExecutionStatus.ERROR, e.getMessage());
                }
            }
        }, delaySeconds, new SplitTaskExecutionListener() {
            @Override
            public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                if (listener != null) {
                    listener.onTaskExecuted();
                }
            }
        });
    }

    @Override
    public void cancel(@Nullable String taskId) {
        if (taskId != null) {
            mTaskExecutor.stopTask(taskId);
        }
    }
}
