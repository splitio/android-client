package io.split.android.client.service.executor.parallel;

import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.logger.Logger;

public class SplitParallelTaskExecutorImpl<T> implements SplitParallelTaskExecutor<T> {

    private static final int TIMEOUT_IN_SECONDS = 5;

    private final int mThreads;

    private final ExecutorService mScheduler;

    public SplitParallelTaskExecutorImpl(int threads, ExecutorService scheduler) {
        mThreads = threads;
        mScheduler = scheduler;
    }

    @Override
    @WorkerThread
    public List<T> execute(Collection<SplitDeferredTaskItem<T>> splitDeferredTaskItems) {
        try {
            List<Future<T>> futures = mScheduler.invokeAll(splitDeferredTaskItems, TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            ArrayList<T> results = new ArrayList<>();

            for (Future<T> future : futures) {
                results.add(future.get());
            }

            return results;
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            Logger.e(e.getLocalizedMessage());

            return new ArrayList<>();
        }
    }

    @Override
    public int getAvailableThreads() {
        return mThreads;
    }
}
