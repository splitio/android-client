package io.split.android.client.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

public class SplitTaskExecutorImpl implements SplitTaskExecutor {
    private static final int SHUTDOWN_WAIT_TIME = 60;
    private static final int CORE_POOL_SIZE = 1;
    private static final String THREAD_NAME_FORMAT = "split-taskExecutor-%d";
    private final PausableScheduledThreadPoolExecutor mScheduler;

    public SplitTaskExecutorImpl() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(false);
        threadFactoryBuilder.setNameFormat(THREAD_NAME_FORMAT);
        mScheduler = new PausableScheduledThreadPoolExecutorImpl(CORE_POOL_SIZE, threadFactoryBuilder.build());
    }

    @Override
    public void schedule(SplitTask task, long initialDelayInSecs, long periodInSecs) {
        if (task != null && !mScheduler.isShutdown()) {
            ScheduledFuture<?> future = mScheduler.scheduleAtFixedRate(new TaskWrapper(task), periodInSecs, initialDelayInSecs, TimeUnit.SECONDS);
        }
    }

    @Override
    public void submit(SplitTask task) {
        if (task != null && !mScheduler.isShutdown()) {
            Future<?> future = mScheduler.submit(new TaskWrapper(task));
        }
    }

    @Override
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
                    mScheduler.shutdownNow(); // Cancel currently executing tasks
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

    static class TaskWrapper implements Runnable {
        private final SplitTask mTask;

        TaskWrapper(SplitTask task) {
            mTask = task;
        }

        @Override
        public void run() {
            mTask.execute();
        }
    }

}
