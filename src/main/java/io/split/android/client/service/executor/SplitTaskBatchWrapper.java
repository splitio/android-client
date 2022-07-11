package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import io.split.android.client.utils.logger.Logger;

class SplitTaskBatchWrapper implements Runnable {
    private final List<SplitTaskBatchItem> mTaskQueue;

    SplitTaskBatchWrapper(List<SplitTaskBatchItem> taskQueue) {
        mTaskQueue = checkNotNull(taskQueue);
    }

    @Override
    public void run() {
        try {
            for (SplitTaskBatchItem enqueued : mTaskQueue) {
                SplitTaskExecutionInfo info = enqueued.getTask().execute();
                SplitTaskExecutionListener listener = enqueued.getListener();
                if (listener != null) {
                    listener.taskExecuted(info);
                }
            }

        } catch (Exception e) {
            Logger.e("An error has occurred while running task on executor: " + e.getLocalizedMessage());
        }
    }
}
