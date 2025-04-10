package io.split.android.client.events;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
import io.split.android.client.service.executor.ThreadFactoryBuilder;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.scheduler.PausableThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableThreadPoolExecutorImpl;

public abstract class BaseEventsManager implements Runnable {

    private final static int QUEUE_CAPACITY = 20;
    // Shared thread factory for all instances
    private static final ThreadFactory EVENTS_THREAD_FACTORY = createThreadFactory();

    protected final ArrayBlockingQueue<SplitInternalEvent> mQueue;

    protected final Set<SplitInternalEvent> mTriggered;

    private static ThreadFactory createThreadFactory() {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Split-FactoryEventsManager-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                        Logger.e("Unexpected error " + e.getLocalizedMessage());
                    }
                });
                return thread;
            }
        };
    }

    public BaseEventsManager() {
        mQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        mTriggered = Collections.newSetFromMap(new ConcurrentHashMap<>());
        launch(EVENTS_THREAD_FACTORY);
    }

    @Override
    public void run() {
        // This code was intentionally designed this way
        // noinspection InfiniteLoopStatement
        while (true) {
            triggerEventsWhenAreAvailable();
        }
    }

    private void launch(ThreadFactory threadFactory) {
        PausableThreadPoolExecutor mScheduler = PausableThreadPoolExecutorImpl.newSingleThreadExecutor(threadFactory);
        mScheduler.submit(this);
        mScheduler.resume();
    }

    protected abstract void triggerEventsWhenAreAvailable();

    protected abstract void notifyInternalEvent(SplitInternalEvent event);
}
