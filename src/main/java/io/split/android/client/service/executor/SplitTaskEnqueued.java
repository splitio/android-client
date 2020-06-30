package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import static androidx.core.util.Preconditions.checkNotNull;

public class SplitTaskEnqueued {
    private final SplitTask task;
    private final WeakReference<SplitTaskExecutionListener> listener;

    public SplitTaskEnqueued(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener listener) {
        this.task = checkNotNull(task);
        this.listener = new WeakReference<>(listener);
    }

    public SplitTask getTask() {
        return task;
    }

    public SplitTaskExecutionListener getListener() {
        return listener.get();
    }
}
