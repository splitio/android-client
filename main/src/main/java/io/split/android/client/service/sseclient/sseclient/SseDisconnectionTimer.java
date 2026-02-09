package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.sseclient.spi.StreamingScheduler;
import io.split.android.client.utils.logger.Logger;

public class SseDisconnectionTimer {

    private final StreamingScheduler mScheduler;
    private final int mInitialDelayInSeconds;
    private String mTaskId;

    public SseDisconnectionTimer(@NonNull StreamingScheduler scheduler, int initialDelayInSeconds) {
        mScheduler = checkNotNull(scheduler);
        mInitialDelayInSeconds = initialDelayInSeconds;
    }

    public void cancel() {
        mScheduler.cancel(mTaskId);
    }

    public void schedule(Runnable task) {
        Logger.v("Scheduling disconnection in " + mInitialDelayInSeconds + " seconds");
        cancel();
        mTaskId = mScheduler.schedule(task, mInitialDelayInSeconds, new StreamingScheduler.TaskExecutionListener() {
            @Override
            public void onTaskExecuted() {
                mTaskId = null;
            }
        });
    }
}
