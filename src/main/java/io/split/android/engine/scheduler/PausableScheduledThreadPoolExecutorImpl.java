package io.split.android.engine.scheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableScheduledThreadPoolExecutorImpl extends ScheduledThreadPoolExecutor implements PausableScheduledThreadPoolExecutor {
    private boolean isPaused;
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpaused = pauseLock.newCondition();
    private final static int POOL_SIZE = 1;

    public static PausableScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return new PausableScheduledThreadPoolExecutorImpl(POOL_SIZE, threadFactory);
    }

    public PausableScheduledThreadPoolExecutorImpl(int corePoolSize,
                                                    ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable task) {
        super.beforeExecute(thread, task);
        pauseLock.lock();
        try {
            while (isPaused) unpaused.await();
        } catch (InterruptedException ie) {
            thread.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    public void resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }
}
