package io.split.android.client.service.executor;

import java.lang.ref.WeakReference;
import java.util.Objects;

import io.split.android.client.utils.logger.Logger;

class TaskWrapper implements Runnable {
    private final SplitTask mTask;
    private final WeakReference<SplitTaskExecutionListener> mExecutionListener;

    TaskWrapper(SplitTask task,
                SplitTaskExecutionListener executionListener) {
        mTask = Objects.requireNonNull(task);
        mExecutionListener = new WeakReference<>(executionListener);
    }

    @Override
    public void run() {
        SplitTaskExecutionInfo info;
        try {
            info = mTask.execute();
        } catch (Exception e) {
            Logger.e("An error has occurred while running task on executor: " + e.getLocalizedMessage());
            info = SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        SplitTaskExecutionListener listener = mExecutionListener.get();
        if (listener != null) {
            listener.taskExecuted(info);
        }
    }
}
