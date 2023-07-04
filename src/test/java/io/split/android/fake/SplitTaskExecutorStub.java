package io.split.android.fake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class SplitTaskExecutorStub implements SplitTaskExecutor {
    @Override
    public String schedule(@NonNull SplitTask task, long initialDelayInSecs, long periodInSecs, @Nullable SplitTaskExecutionListener executionListener) {
        return null;
    }

    @Override
    public String schedule(@NonNull SplitTask task, long initialDelayInSecs, @Nullable SplitTaskExecutionListener executionListener) {
        return "";
    }

    @Override
    public void submit(@NonNull SplitTask task, @Nullable SplitTaskExecutionListener executionListener) {
        SplitTaskExecutionInfo execute = task.execute();
        if (executionListener != null) {
            if (execute != null) {
                executionListener.taskExecuted(execute);
            }
        }
    }

    @Override
    public void executeSerially(List<SplitTaskBatchItem> tasks) {
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void stopTask(String taskId) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void submitOnMainThread(SplitTask splitTask) {

    }
}
