package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.utils.Logger;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class RetryBackoffCounterTimer implements SplitTaskExecutionListener {

    private SplitTaskExecutor mTaskExecutor;
    private ReconnectBackoffCounter mStreamingBackoffCounter;
    private SplitTask mTask;
    private SplitTaskExecutionListener mListener;
    private String mTaskId;

    public RetryBackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull ReconnectBackoffCounter streamingBackoffCounter) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mStreamingBackoffCounter = checkNotNull(streamingBackoffCounter);
    }

    public void setTask(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener listener) {
        mTask = checkNotNull(task);
        mListener = listener;
    }

    public void stop() {
        if(mTask == null) {
            return;
        }
        mStreamingBackoffCounter.resetCounter();
        mTaskExecutor.stopTask(mTaskId);
        mTaskId = null;
    }

    public void start() {
        if(mTask == null || mTaskId != null) {
            return;
        }
        mTaskId = mTaskExecutor.schedule(mTask, 0L, this);
    }

    private void schedule() {
        long retryTime = mStreamingBackoffCounter.getNextRetryTime();
        Logger.d(String.format("Retrying %s task in %d seconds", mTask.getClass().getSimpleName(),  retryTime));
        mTaskId = mTaskExecutor.schedule(mTask, retryTime, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
        if(taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            schedule();
            return;
        }
        mStreamingBackoffCounter.resetCounter();
        if(mListener != null) {
            mListener.taskExecuted(SplitTaskExecutionInfo.success(taskInfo.getTaskType()));
        }
    }
}
