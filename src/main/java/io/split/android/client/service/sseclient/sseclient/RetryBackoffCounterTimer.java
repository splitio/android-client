package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.utils.logger.Logger;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryBackoffCounterTimer implements SplitTaskExecutionListener {

    private static final int DEFAULT_MAX_ATTEMPTS = -1;

    private final SplitTaskExecutor mTaskExecutor;
    private final BackoffCounter mBackoffCounter;
    private final int mRetryAttemptsLimit;
    private final AtomicInteger mCurrentAttempts = new AtomicInteger(0);
    private SplitTask mTask;
    private SplitTaskExecutionListener mListener;
    private String mTaskId;

    /**
     * Creates an instance which retries tasks indefinitely, using the strategy defined by backoffCounter.
     *
     * @param taskExecutor   Implementation of SplitTaskExecutor.
     * @param backoffCounter Will determine the retry interval.
     */
    public RetryBackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull BackoffCounter backoffCounter) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mBackoffCounter = checkNotNull(backoffCounter);
        mRetryAttemptsLimit = DEFAULT_MAX_ATTEMPTS;
    }

    /**
     * Creates an instance which retries tasks up to the number of times specified by retryAttemptsLimit.
     *
     * @param taskExecutor       Implementation of SplitTaskExecutor.
     * @param backoffCounter     Will determine the retry interval.
     * @param retryAttemptsLimit Maximum number of attempts for task retry.
     */
    public RetryBackoffCounterTimer(@NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull BackoffCounter backoffCounter,
                                    int retryAttemptsLimit) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mBackoffCounter = checkNotNull(backoffCounter);
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
        if (mTask == null) {
            return;
        }
        mTaskExecutor.stopTask(mTaskId);
        mTaskId = null;
    }

    synchronized public void start() {
        if (mTask == null || mTaskId != null) {
            return;
        }
        mBackoffCounter.resetCounter();
        mCurrentAttempts.incrementAndGet();
        mTaskId = mTaskExecutor.schedule(mTask, 0L, this);
    }

    synchronized private void schedule() {
        if (mTask == null) {
            return;
        }
        long retryTime = mBackoffCounter.getNextRetryTime();
        Logger.d(String.format("Retrying %s task in %d seconds", mTask.getClass().getSimpleName(), retryTime));
        mCurrentAttempts.incrementAndGet();
        mTaskId = mTaskExecutor.schedule(mTask, retryTime, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
        if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR &&
                (taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY) == null ||
                        Boolean.FALSE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY)))) {

            if (mRetryAttemptsLimit == DEFAULT_MAX_ATTEMPTS || mCurrentAttempts.get() < mRetryAttemptsLimit) {
                schedule();
            }

            return;
        }

        mBackoffCounter.resetCounter();

        if (mListener != null) {
            if (taskInfo.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
                mListener.taskExecuted(SplitTaskExecutionInfo.success(taskInfo.getTaskType()));
            } else if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                mListener.taskExecuted(SplitTaskExecutionInfo.error(taskInfo.getTaskType()));
            }
        }
    }
}
