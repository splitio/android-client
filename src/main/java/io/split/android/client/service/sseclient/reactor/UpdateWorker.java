package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.logger.Logger;

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
                    Logger.e("Update worker did not terminate");
                }
            } catch (InterruptedException ie) {
                mExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitForNotifications() {
        if (!mExecutorService.isShutdown()) {
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            onWaitForNotificationLoop();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    protected abstract void onWaitForNotificationLoop() throws InterruptedException;
}
