package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;

public abstract class SplitBaseTaskExecutor implements SplitTaskExecutor {

    private static final int SHUTDOWN_WAIT_TIME = 15;
    private final PausableScheduledThreadPoolExecutor mScheduler;
    private final Map<String, ScheduledFuture> mScheduledTasks;

    public SplitBaseTaskExecutor() {
        mScheduler = buildScheduler();
        mScheduledTasks = new ConcurrentHashMap<>();
    }

    @NonNull
    protected abstract PausableScheduledThreadPoolExecutor buildScheduler();

    @Nullable
    @Override
    public String schedule(@NonNull SplitTask task,
                           long initialDelayInSecs,
                           long periodInSecs,
                           @Nullable SplitTaskExecutionListener executionListener
    ) {
        checkNotNull(task);
        checkArgument(periodInSecs > 0);

        String taskId = null;
        if (!mScheduler.isShutdown()) {
            ScheduledFuture taskFuture = mScheduler.scheduleAtFixedRate(
                    new TaskWrapper(task, executionListener),
                    initialDelayInSecs, periodInSecs, TimeUnit.SECONDS);
            taskId = UUID.randomUUID().toString();
            mScheduledTasks.put(taskId, taskFuture);
        }
        return taskId;
    }

    @Nullable
    @Override
    public String schedule(@NonNull SplitTask task,
                           long initialDelayInSecs,
                           @Nullable SplitTaskExecutionListener executionListener
    ) {
        checkNotNull(task);
        String taskId = null;
        if (!mScheduler.isShutdown()) {
            ScheduledFuture taskFuture = mScheduler.schedule(
                    new TaskWrapper(task, executionListener),
                    initialDelayInSecs, TimeUnit.SECONDS);
            taskId = UUID.randomUUID().toString();
            mScheduledTasks.put(taskId, taskFuture);
        }
        return taskId;
    }

    @Override
    public void submit(@NonNull SplitTask task,
                       @Nullable SplitTaskExecutionListener executionListener) {
        checkNotNull(task);
        if (!mScheduler.isShutdown()) {
            mScheduler.submit(new TaskWrapper(task, executionListener));
        }
    }

    @Override
    public void stopTask(String taskId) {
        if(taskId == null ) {
            return;
        }
        ScheduledFuture taskFuture = mScheduledTasks.get(taskId);
        if(taskFuture != null) {
            taskFuture.cancel(false);
        }
        mScheduledTasks.remove(taskId);
    }

    @Override
    public void executeSerially(List<SplitTaskBatchItem> taskQueue) {
        SplitTaskBatchWrapper queue = new SplitTaskBatchWrapper(taskQueue);
        mScheduler.submit(queue);
    }

    public void pause() {
        mScheduler.pause();
    }

    @Override
    public void resume() {
        mScheduler.resume();
    }

    @Override
    public void stop() {
        if (!mScheduler.isShutdown()) {
            mScheduler.shutdown();
            try {
                if (!mScheduler.awaitTermination(SHUTDOWN_WAIT_TIME, TimeUnit.SECONDS)) {
                    mScheduler.shutdownNow();
                    if (!mScheduler.awaitTermination(SHUTDOWN_WAIT_TIME, TimeUnit.SECONDS)) {
                        Logger.e("Split task executor did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                mScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
