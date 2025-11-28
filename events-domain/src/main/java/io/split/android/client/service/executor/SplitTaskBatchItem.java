package io.split.android.client.service.executor;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public class SplitTaskBatchItem {
    private final SplitTask task;
    private final WeakReference<SplitTaskExecutionListener> listener;

    public SplitTaskBatchItem(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener listener) {
        this.task = requireNonNull(task);
        this.listener = new WeakReference<>(listener);
    }

    public SplitTask getTask() {
        return task;
    }

    public SplitTaskExecutionListener getListener() {
        return listener.get();
    }
}
