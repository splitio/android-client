package io.split.android.client.service.executor;

import java.util.List;
import java.util.Objects;

import io.split.android.client.utils.logger.Logger;

class SplitTaskBatchWrapper implements Runnable {
    private final List<SplitTaskBatchItem> mTaskQueue;

    SplitTaskBatchWrapper(List<SplitTaskBatchItem> taskQueue) {
        mTaskQueue = Objects.requireNonNull(taskQueue);
    }

    @Override
    public void run() {
        for (SplitTaskBatchItem enqueued : mTaskQueue) {
            try {
                SplitTaskExecutionInfo info = enqueued.getTask().execute();
                SplitTaskExecutionListener listener = enqueued.getListener();
                if (listener != null) {
                    listener.taskExecuted(info);
                }
            } catch (Exception e) {
                Logger.e("An error has occurred while running task on executor: " + e.getLocalizedMessage());
            }
        }
    }
}
