package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class BackoffCounterTimer implements SplitTaskExecutionListener {

    private SplitTaskExecutor mTaskExecutor;
    private ReconnectBackoffCounter mStreamingBackoffCounter;
    private SplitTask mTask;
    String mTaskId;

    public BackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                               @NonNull ReconnectBackoffCounter streamingBackoffCounter) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mStreamingBackoffCounter = checkNotNull(streamingBackoffCounter);
    }

    public void setTask(@NonNull SplitTask task) {
        mTask = checkNotNull(task);
    }

    public void cancel() {
        if(mTask == null) {
            return;
        }
        mStreamingBackoffCounter.resetCounter();
        mTaskExecutor.stopTask(mTaskId);
    }

    public void schedule() {
        if(mTask == null) {
            return;
        }

        cancel();
        mTaskId = mTaskExecutor.schedule(mTask, mStreamingBackoffCounter.getNextRetryTime(), this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
    }
}
