package io.split.android.client.events.executors;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

abstract class ClientEventSplitTask implements SplitTask {

    protected final SplitEventTask mTask;
    protected final SplitClient mSplitClient;

    ClientEventSplitTask(SplitEventTask task, SplitClient client) {
        mTask = task;
        mSplitClient = client;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            action();
        } catch (Exception e) {
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }

    protected abstract void action();
}
