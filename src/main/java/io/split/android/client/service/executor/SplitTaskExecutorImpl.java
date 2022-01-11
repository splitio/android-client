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

import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutorImpl implements SplitTaskExecutor {
    private static final int SHUTDOWN_WAIT_TIME = 15;
    private static final int MIN_THREADPOOL_SIZE_WHEN_IDLE = 6;
    private static final String THREAD_NAME_FORMAT = "split-taskExecutor-%d";
    private final PausableScheduledThreadPoolExecutor mScheduler;
    private final Map<String, ScheduledFuture> mScheduledTasks;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public SplitTaskExecutorImpl(@NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
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
                    new TaskWrapper(task, executionListener, mTelemetryRuntimeProducer),
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
                    new TaskWrapper(task, executionListener, mTelemetryRuntimeProducer),
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
            mScheduler.submit(new TaskWrapper(task, executionListener, mTelemetryRuntimeProducer));
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
        SplitTaskBatchWrapper queue = new SplitTaskBatchWrapper(taskQueue, mTelemetryRuntimeProducer);
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

    private static class TaskWrapper implements Runnable {
        private final SplitTask mTask;
        private WeakReference<SplitTaskExecutionListener> mExecutionListener;
        private final WeakReference<TelemetryRuntimeProducer> mTelemetryRuntimeProducer;

        TaskWrapper(SplitTask task,
                    SplitTaskExecutionListener executionListener,
                    TelemetryRuntimeProducer telemetryRuntimeProducer) {
            mTask = checkNotNull(task);
            mExecutionListener = new WeakReference<>(executionListener);
            mTelemetryRuntimeProducer = new WeakReference<>(telemetryRuntimeProducer);
        }

        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                SplitTaskExecutionInfo info = mTask.execute();
                SplitTaskExecutionListener listener = mExecutionListener.get();
                if (listener != null) {
                    listener.taskExecuted(info);
                }

                recordLatency(startTime, info, mTelemetryRuntimeProducer.get());
            } catch (Exception e) {
                Logger.e("An error has ocurred while running task on executor: " + e.getLocalizedMessage());
            }

        }

        private void recordLatency(long startTime, SplitTaskExecutionInfo info, TelemetryRuntimeProducer telemetryRuntimeProducer) {
            long latency = System.currentTimeMillis() - startTime;
            if (telemetryRuntimeProducer != null) {
                OperationType operationType = OperationType.getFromTaskType(info.getTaskType());
                if (operationType != null) {
                    telemetryRuntimeProducer.recordSyncLatency(operationType, latency);
                }
            }
        }
    }

    private static class SplitTaskBatchWrapper implements Runnable {
        private final List<SplitTaskBatchItem> mTaskQueue;
        private final WeakReference<TelemetryRuntimeProducer> mTelemetryRuntimeProducer;

        SplitTaskBatchWrapper(List<SplitTaskBatchItem> taskQueue, TelemetryRuntimeProducer telemetryRuntimeProducer) {
            mTaskQueue = checkNotNull(taskQueue);
            mTelemetryRuntimeProducer = new WeakReference<>(telemetryRuntimeProducer);
        }

        @Override
        public void run() {
            try {
                for(SplitTaskBatchItem enqueued : mTaskQueue) {
                    long startTime = System.currentTimeMillis();
                    SplitTaskExecutionInfo info = enqueued.getTask().execute();
                    SplitTaskExecutionListener listener = enqueued.getListener();
                    if (listener != null) {
                        listener.taskExecuted(info);
                    }

                    recordLatency(startTime, info, mTelemetryRuntimeProducer.get());
                }

            } catch (Exception e) {
                Logger.e("An error has ocurred while running task on executor: " + e.getLocalizedMessage());
            }
        }

        private void recordLatency(long startTime, SplitTaskExecutionInfo info, TelemetryRuntimeProducer telemetryRuntimeProducer) {
            long latency = System.currentTimeMillis() - startTime;
            if (telemetryRuntimeProducer != null) {
                OperationType operationType = OperationType.getFromTaskType(info.getTaskType());
                if (operationType != null) {
                    telemetryRuntimeProducer.recordSyncLatency(operationType, latency);
                }
            }
        }
    }
}
