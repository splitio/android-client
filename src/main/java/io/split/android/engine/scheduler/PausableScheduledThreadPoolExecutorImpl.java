package io.split.android.engine.scheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;

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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: About to acquire pauseLock"));
        long lockStartTime = System.currentTimeMillis();
        pauseLock.lock();
        System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: Acquired pauseLock in " + 
                (System.currentTimeMillis() - lockStartTime) + "ms"));
        try {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: Setting isPaused=false"));
            isPaused = false;
            System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: About to signal all waiting threads"));
            long signalStartTime = System.currentTimeMillis();
            unpaused.signalAll();
            System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: Signaled all threads in " + 
                    (System.currentTimeMillis() - signalStartTime) + "ms"));
        } finally {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: About to release pauseLock"));
            long unlockStartTime = System.currentTimeMillis();
            pauseLock.unlock();
            System.out.println(StartupTimeTracker.getElapsedTimeLog("PausableScheduledThreadPoolExecutorImpl: Released pauseLock in " + 
                    (System.currentTimeMillis() - unlockStartTime) + "ms"));
        }
    }
}
