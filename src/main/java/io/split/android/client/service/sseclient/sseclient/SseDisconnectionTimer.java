package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.utils.logger.Logger;

public class SseDisconnectionTimer implements SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final int mInitialDelayInSeconds;
    private String mTaskId;

    public SseDisconnectionTimer(@NonNull SplitTaskExecutor taskExecutor, int initialDelayInSeconds) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mInitialDelayInSeconds = initialDelayInSeconds;
    }

    public void cancel() {
        if (mTaskId != null) {
            mTaskExecutor.stopTask(mTaskId);
        }
    }

    public void schedule(SplitTask task) {
        Logger.v("Scheduling disconnection in " + mInitialDelayInSeconds + " seconds");
        cancel();
        mTaskId = mTaskExecutor.schedule(task, mInitialDelayInSeconds, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
    }
}
