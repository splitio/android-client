package io.split.android.client.service.synchronizer.connectivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.logger.Logger;

public class Debouncer {
    private final ScheduledExecutorService mScheduler;
    private final long mDelay;
    private final TimeUnit mTimeUnit;
    private Runnable mTask;
    private ScheduledFuture<?> mScheduledFuture;

    public Debouncer(long delay, TimeUnit unit) {
        mScheduler = Executors.newScheduledThreadPool(1);
        mDelay = delay;
        mTimeUnit = unit;
    }

    public void setTask(Runnable task) {
        mTask = task;
        debounce();
    }

    private synchronized void debounce() {
        if (mScheduledFuture != null) {
            Logger.i("NETWORK: Debouncer: Cancelling previous scheduled task");
            mScheduledFuture.cancel(false);
        }

        Logger.e("NETWORK: Debouncer: Scheduling new task in " + mDelay + " ms");
        mScheduledFuture = mScheduler.schedule(mTask, mDelay, mTimeUnit);
    }

    public void shutdown() {
        mScheduler.shutdown();
    }
}
