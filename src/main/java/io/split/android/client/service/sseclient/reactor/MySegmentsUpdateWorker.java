package io.split.android.client.service.sseclient.reactor;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.split.android.client.service.Synchronizer;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class MySegmentsUpdateWorker extends UpdateWorker {

    /***
     * This class will be in charge of update my segments when a new notification arrived.
     */

    private final Synchronizer mSynchronizer;
    private final BlockingQueue<MySegmentChangeNotification> mNotificationsQueue;

    public MySegmentsUpdateWorker(
            @NonNull Synchronizer synchronizer,
            @NonNull BlockingQueue<MySegmentChangeNotification> notificationsQueue) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
    }

    @Override
    protected void onWaitForNotificationLoop() {
        try {
            mNotificationsQueue.take();
            mSynchronizer.syncronizeMySegments();
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueing polling task.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}
