package io.split.android.client.service.executor.parallel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplitParallelTaskExecutorFactoryImpl implements SplitParallelTaskExecutorFactory {

    private final int mThreads = Runtime.getRuntime().availableProcessors();

    private final ExecutorService mScheduler = Executors.newFixedThreadPool(mThreads);

    @Override
    public <T> SplitParallelTaskExecutor<List<T>> createForList(Class<T> type) {
        return new SplitParallelTaskExecutorImpl<>(mThreads, mScheduler);
    }

    @Override
    public <T> SplitParallelTaskExecutor<T> create(Class<T> type) {
        return new SplitParallelTaskExecutorImpl<>(mThreads, mScheduler);
    }
}
