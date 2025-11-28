package io.split.android.client.events.executors;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

class ClientEventSplitTask implements SplitTask {

    private final SplitEventTask mTask;
    private final SplitClient mSplitClient;
    private final boolean mIsMainThread;

    ClientEventSplitTask(SplitEventTask task, SplitClient client, boolean isMainThread) {
        mTask = task;
        mSplitClient = client;
        mIsMainThread = isMainThread;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            if (mIsMainThread) {
                mTask.onPostExecutionView(mSplitClient);
            } else {
                mTask.onPostExecution(mSplitClient);
            }
        } catch (Exception e) {
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
