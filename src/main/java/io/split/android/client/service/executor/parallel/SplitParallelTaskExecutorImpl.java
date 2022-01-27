package io.split.android.client.service.executor.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.split.android.client.utils.Logger;

public class SplitParallelTaskExecutorImpl<T> implements SplitParallelTaskExecutor<T> {

    private static final int nThreads = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService mScheduler = Executors.newFixedThreadPool(nThreads);

    @Override
    public List<T> execute(Collection<SplitDeferredTaskItem<T>> splitDeferredTaskItems) {
        try {
            List<Future<T>> futures = mScheduler.invokeAll(splitDeferredTaskItems);
            ArrayList<T> results = new ArrayList<>();

            for (Future<T> future : futures) {
                results.add(future.get());
            }

            return results;
        } catch (InterruptedException | ExecutionException e) {
            Logger.e(e.getLocalizedMessage());

            return new ArrayList<>();
        }
    }
}
