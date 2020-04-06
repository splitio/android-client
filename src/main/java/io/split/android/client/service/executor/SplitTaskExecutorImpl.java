package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutorImpl implements SplitTaskExecutor {
    private static final int SHUTDOWN_WAIT_TIME = 60;
    private static final int MIN_THREADPOOL_SIZE_WHEN_IDLE = 1;
    private static final String THREAD_NAME_FORMAT = "split-taskExecutor-%d";
    private final PausableScheduledThreadPoolExecutor mScheduler;
    private final Map<String, ScheduledFuture> mScheduledTasks;

    public SplitTaskExecutorImpl() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat(THREAD_NAME_FORMAT);
        mScheduler = new PausableScheduledThreadPoolExecutorImpl(MIN_THREADPOOL_SIZE_WHEN_IDLE, threadFactoryBuilder.build());
        mScheduledTasks = new ConcurrentHashMap<>();
    }

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
        ScheduledFuture taskFuture = mScheduledTasks.get(taskId);
        taskFuture.cancel(false);
        mScheduledTasks.remove(taskId);
    }

    @Override
    public void stopTasks(List<String> taskIds) {
        for (String taskId : taskIds) {
            stopTask(taskId);
        }
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

    private class TaskWrapper implements Runnable {
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
                Logger.e("An error has ocurred while running task on executor: " + e.getLocalizedMessage());
            }

        }
    }
}
