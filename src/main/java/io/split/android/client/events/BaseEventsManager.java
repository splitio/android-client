package io.split.android.client.events;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.ThreadFactoryBuilder;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;

import io.split.android.client.utils.ConcurrentSet;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.scheduler.PausableThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableThreadPoolExecutorImpl;

public abstract class BaseEventsManager implements Runnable {

    private final static int QUEUE_CAPACITY = 20;

    protected final ArrayBlockingQueue<SplitInternalEvent> mQueue;

    protected final Set<SplitInternalEvent> mTriggered;

    public BaseEventsManager() {

        mQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        mTriggered = new ConcurrentSet<>();

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-FactoryEventsManager-%d")
                .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                        Logger.e("Unexpected error " + e.getLocalizedMessage());
                    }
                })
                .build();
        launch(threadFactory);
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
