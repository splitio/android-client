package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.utils.Logger;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryBackoffCounterTimer implements SplitTaskExecutionListener {

    private static final int DEFAULT_MAX_ATTEMPTS = -1;

    private final SplitTaskExecutor mTaskExecutor;
    private final BackoffCounter mStreamingBackoffCounter;
    private final int mRetryAttemptsLimit;
    private final AtomicInteger mCurrentAttempts = new AtomicInteger(0);
    private SplitTask mTask;
    private SplitTaskExecutionListener mListener;
    private String mTaskId;

    public RetryBackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull BackoffCounter streamingBackoffCounter) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mStreamingBackoffCounter = checkNotNull(streamingBackoffCounter);
        mRetryAttemptsLimit = DEFAULT_MAX_ATTEMPTS;
    }

    public RetryBackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull BackoffCounter streamingBackoffCounter,
                                    int retryAttemptsLimit) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mStreamingBackoffCounter = checkNotNull(streamingBackoffCounter);
        mRetryAttemptsLimit = retryAttemptsLimit;
    }

    synchronized public void setTask(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener listener) {
        mTask = checkNotNull(task);
        mListener = listener;
    }

    synchronized public void setTask(@NonNull SplitTask task) {
        setTask(task, null);
    }

    synchronized public void stop() {
        if(mTask == null) {
            return;
        }
        mTaskExecutor.stopTask(mTaskId);
        mTaskId = null;
    }

    synchronized public void start() {
        if(mTask == null || mTaskId != null) {
            return;
        }
        mStreamingBackoffCounter.resetCounter();
        mCurrentAttempts.incrementAndGet();
        mTaskId = mTaskExecutor.schedule(mTask, 0L, this);
    }

    synchronized private void schedule() {
        if (mTask == null) {
            return;
        }
        long retryTime = mStreamingBackoffCounter.getNextRetryTime();
        Logger.d(String.format("Retrying %s task in %d seconds", mTask.getClass().getSimpleName(), retryTime));
        mCurrentAttempts.incrementAndGet();
        mTaskId = mTaskExecutor.schedule(mTask, retryTime, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
        if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            if (mRetryAttemptsLimit == DEFAULT_MAX_ATTEMPTS || mCurrentAttempts.get() < mRetryAttemptsLimit) {
                schedule();
            }
            return;
        }

        mStreamingBackoffCounter.resetCounter();
        if (mListener != null) {
            mListener.taskExecuted(SplitTaskExecutionInfo.success(taskInfo.getTaskType()));
        }
    }
}
