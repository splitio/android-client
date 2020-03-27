package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static androidx.core.util.Preconditions.checkNotNull;

public abstract class UpdateWorker {

    /***
     * Base component having common update workers component.
     * Specific update workers should extend this class
     */
    private final ExecutorService mExecutorService;

    public UpdateWorker() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    protected void waitForNotifications() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    onWaitForNotificationLoop();
                }
            }
        });
    }

    protected abstract void onWaitForNotificationLoop();
}
