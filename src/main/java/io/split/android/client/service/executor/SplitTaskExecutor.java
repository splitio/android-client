package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface SplitTaskExecutor {
    String schedule(@NonNull SplitTask task,
                  long initialDelayInSecs,
                  long periodInSecs,
                  @Nullable SplitTaskExecutionListener executionListener);

    void submit(@NonNull SplitTask task,
                @Nullable SplitTaskExecutionListener executionListener);

    void execute(@NonNull SplitTask task, @NonNull String queueName);

    void pause();

    void resume();

    void stopTasks(List<String> taskIds);

    void stop();
}
