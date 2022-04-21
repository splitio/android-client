package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;

import io.split.android.client.utils.Logger;

class TaskWrapper implements Runnable {
    private final SplitTask mTask;
    private WeakReference<SplitTaskExecutionListener> mExecutionListener;

    TaskWrapper(SplitTask task,
                SplitTaskExecutionListener executionListener) {
        mTask = checkNotNull(task);
        mExecutionListener = new WeakReference<>(executionListener);
    }

    @Override
    public void run() {
        try {
            SplitTaskExecutionInfo info = mTask.execute();
            SplitTaskExecutionListener listener = mExecutionListener.get();
            if (listener != null) {
                listener.taskExecuted(info);
            }

        } catch (Exception e) {
            Logger.e("An error has occurred while running task on executor: " + e.getLocalizedMessage());
        }
    }
}
