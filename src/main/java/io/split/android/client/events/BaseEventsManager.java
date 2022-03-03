package io.split.android.client.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import io.split.android.client.utils.ConcurrentSet;
import io.split.android.client.utils.Logger;

public abstract class BaseEventsManager implements Runnable {

    private final static int QUEUE_CAPACITY = 20;

    protected final ArrayBlockingQueue<SplitInternalEvent> _queue;

    protected final Set<SplitInternalEvent> _triggered;

    public BaseEventsManager() {

        _queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        _triggered = new ConcurrentSet<>();

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-FactoryEventsManager-%d")
                .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        Logger.e("Unexpected error " + e.getLocalizedMessage());
                    }
                })
                .build();
        ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);

        mScheduler.submit(this);
    }

    @Override
    public void run() {
        // This code was intentionally designed this way
        // noinspection InfiniteLoopStatement
        while (true) {
            triggerEventsWhenAreAvailable();
        }
    }

    protected abstract void triggerEventsWhenAreAvailable();

    protected abstract void notifyInternalEvent(SplitInternalEvent event);
}
