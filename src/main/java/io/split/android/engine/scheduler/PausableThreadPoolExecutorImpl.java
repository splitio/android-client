package io.split.android.engine.scheduler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableThreadPoolExecutorImpl extends ThreadPoolExecutor implements PausableThreadPoolExecutor {
    private boolean isPaused;
    private ReentrantLock pauseLock = new ReentrantLock();
    private Condition unpaused = pauseLock.newCondition();
    private final static int POOL_SIZE = 1;

    public static PausableThreadPoolExecutorImpl newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new PausableThreadPoolExecutorImpl(POOL_SIZE, threadFactory);
    }

    public PausableThreadPoolExecutorImpl(int corePoolSize,
                                           ThreadFactory threadFactory) {
        super(corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

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
