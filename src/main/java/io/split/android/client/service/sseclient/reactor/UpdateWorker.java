package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;

public abstract class UpdateWorker {

    /***
     * Base component having common update workers component.
     * Specific update workers should extend this class
     */
    private final ExecutorService mExecutorService;
    private static final int SHUTDOWN_WAIT_TIME = 30;

    public UpdateWorker() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        waitForNotifications();
    }

    public void stop() {
        if (!mExecutorService.isShutdown()) {
            try {
                mExecutorService.shutdownNow();
                if (!mExecutorService.awaitTermination(SHUTDOWN_WAIT_TIME, TimeUnit.SECONDS)) {
                    Logger.e("Split task executor did not terminate");
                }
            } catch (InterruptedException ie) {
                mExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitForNotifications() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    onWaitForNotificationLoop();
                }
            }
        });
    }

    protected abstract void onWaitForNotificationLoop();
}
