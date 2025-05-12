package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.utils.logger.Logger;

public class BackoffCounterTimer implements SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final BackoffCounter mBackoffCounter;
    private SplitTask mTask;
    String mTaskId;

    public BackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                               @NonNull BackoffCounter streamingBackoffCounter) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mBackoffCounter = checkNotNull(streamingBackoffCounter);
    }

    public void setTask(@NonNull SplitTask task) {
        mTask = checkNotNull(task);
    }

    public void cancel() {
        if(mTask == null) {
            return;
        }
        mBackoffCounter.resetCounter();
        mTaskExecutor.stopTask(mTaskId);
        mTaskId = null;
    }

    public void schedule() {
        // mTaskId != null means task already scheduled, so return to avoid schedule a second one
        if(mTask == null || mTaskId != null) {
            return;
        }

        long retryTime = mBackoffCounter.getNextRetryTime();
        Logger.d(String.format("Retrying reconnection in %d seconds", retryTime));
        mTaskId = mTaskExecutor.schedule(mTask, retryTime, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
    }
}
