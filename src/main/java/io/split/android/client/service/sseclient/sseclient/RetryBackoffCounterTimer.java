package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.utils.logger.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryBackoffCounterTimer implements SplitTaskExecutionListener {

    private static final int DEFAULT_MAX_ATTEMPTS = -1;

    private final SplitTaskExecutor mTaskExecutor;
    private final BackoffCounter mBackoffCounter;
    private final int mRetryAttemptsLimit;
    private final AtomicInteger mCurrentAttempts;
    private Long mInitialDelayInSeconds;
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
        this(taskExecutor, backoffCounter, DEFAULT_MAX_ATTEMPTS);
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
        mCurrentAttempts = new AtomicInteger(0);
        mTaskExecutor = checkNotNull(taskExecutor);
        mBackoffCounter = checkNotNull(backoffCounter);
        mRetryAttemptsLimit = retryAttemptsLimit;
    }

    synchronized public void setTask(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener listener) {
        setTask(task, ServiceConstants.NO_INITIAL_DELAY, listener);
    }

    synchronized public void setTask(@NonNull SplitTask task) {
        setTask(task, null);
    }

    synchronized public void setTask(@NonNull SplitTask task, @Nullable Long initialDelayInMillis, @Nullable SplitTaskExecutionListener listener) {
        mTask = checkNotNull(task);
        mListener = listener;
        if (initialDelayInMillis != null) {
            mInitialDelayInSeconds = TimeUnit.MILLISECONDS.toSeconds(initialDelayInMillis);
        } else {
            mInitialDelayInSeconds = 0L;
        }
        mCurrentAttempts.set(0);
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
        mTaskId = mTaskExecutor.schedule(mTask, mInitialDelayInSeconds, this);
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
        if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            if (taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY) == null ||
                    Boolean.FALSE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {

                if (mRetryAttemptsLimit == DEFAULT_MAX_ATTEMPTS || mCurrentAttempts.get() < mRetryAttemptsLimit) {
                    schedule();
                }

                return;
            } else if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                if (mListener != null) {
                    mListener.taskExecuted(taskInfo);
                }
            }
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
