package io.split.android.client.metrics;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.android.client.utils.Logger;
import io.split.android.engine.metrics.Metrics;


import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FireAndForgetMetrics implements Metrics, Closeable {

    private final ExecutorService _executorService;
    private final Metrics _delegate;

    public static FireAndForgetMetrics instance(Metrics delegate, int numberOfThreads, int queueSize) {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat("split-fireAndForgetMetrics-%d");
        threadFactoryBuilder.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.e(e, "Error in thread: %s", t.getName());
            }
        });

        final ExecutorService executorService = new ThreadPoolExecutor(numberOfThreads,
                numberOfThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(queueSize),
                threadFactoryBuilder.build(),
                new ThreadPoolExecutor.DiscardPolicy());


        return new FireAndForgetMetrics(delegate, executorService);
    }

    private FireAndForgetMetrics(Metrics delegate, ExecutorService executorService) {
        _delegate = delegate;
        _executorService = executorService;
    }


    @Override
    public void count(String counter, long delta) {
        try {
            _executorService.submit(new CountRunnable(_delegate, counter, delta));
        } catch (Throwable t) {
            Logger.w(t, "CountRunnable failed");
        }
    }

    @Override
    public void time(String operation, long timeInMs) {
        try {
            _executorService.submit(new TimeRunnable(_delegate, operation, timeInMs));
        } catch (Throwable t) {
            Logger.w(t, "TimeRunnable failed");
        }
    }

    public void close() {
        _executorService.shutdown();
        try {
            if (!_executorService.awaitTermination(10L, TimeUnit.SECONDS)) { //optional *
                Logger.w("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = _executorService.shutdownNow(); //optional **
                Logger.w("Executor was abruptly shut down. These tasks will not be executed: %s", droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Thread.currentThread().interrupt();
        }
    }


    private static final class CountRunnable implements Runnable {

        private final Metrics _delegate;
        private final String _name;
        private final long _delta;

        public CountRunnable(Metrics delegate, String name, long delta) {
            _delegate = delegate;
            _name = name;
            _delta = delta;
        }

        @Override
        public void run() {
            _delegate.count(_name, _delta);
        }
    }

    private static final class TimeRunnable implements Runnable {

        private final Metrics _delegate;
        private final String _name;
        private final long _timeInMs;

        public TimeRunnable(Metrics delegate, String name, long timeInMs) {
            _delegate = delegate;
            _name = name;
            _timeInMs = timeInMs;
        }

        @Override
        public void run() {
            _delegate.time(_name, _timeInMs);
        }
    }

}
