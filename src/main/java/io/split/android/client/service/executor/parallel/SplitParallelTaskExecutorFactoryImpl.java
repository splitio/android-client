package io.split.android.client.service.executor.parallel;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.service.executor.ThreadFactoryBuilder;
import io.split.android.client.utils.logger.Logger;

public class SplitParallelTaskExecutorFactoryImpl implements SplitParallelTaskExecutorFactory {

    private final int mThreads = Runtime.getRuntime().availableProcessors();

    private final ExecutorService mScheduler = Executors.newFixedThreadPool(mThreads,
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                private final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                        Logger.e("Unexpected error " + e.getLocalizedMessage());
                    }
                };
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("Split-ParallelTaskExecutor-" + threadNumber.getAndIncrement());
                    thread.setUncaughtExceptionHandler(exceptionHandler);
                    return thread;
                }
            });

    @Override
    public <T> SplitParallelTaskExecutor<List<T>> createForList(Class<T> type) {
        return new SplitParallelTaskExecutorImpl<>(mThreads, mScheduler);
    }

    @Override
    public <T> SplitParallelTaskExecutor<T> create(Class<T> type) {
        return new SplitParallelTaskExecutorImpl<>(mThreads, mScheduler);
    }

    @Override
    public <T> SplitParallelTaskExecutor<T> create(Class<T> type, int timeoutInSeconds) {
        return new SplitParallelTaskExecutorImpl<>(mThreads, mScheduler, timeoutInSeconds);
    }
}
