package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface SplitTaskExecutor {
    String schedule(@NonNull SplitTask task,
                    long initialDelayInSecs,
                    long periodInSecs,
                    @Nullable SplitTaskExecutionListener executionListener);

    String schedule(@NonNull SplitTask task,
                    long initialDelayInSecs,
                    @Nullable SplitTaskExecutionListener executionListener);

    void submit(@NonNull SplitTask task,
                @Nullable SplitTaskExecutionListener executionListener);

    void executeSerially(List<SplitTaskBatchItem> tasks);

    void pause();

    void resume();

    void stopTask(String taskId);

    void stop();

    void submitOnMainThread(SplitTask splitTask);
}
