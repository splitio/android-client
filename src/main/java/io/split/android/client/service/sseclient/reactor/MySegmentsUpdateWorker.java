package io.split.android.client.service.sseclient.reactor;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.utils.Logger;

/**
 * This class will be in charge of update my segments when a new notification arrived.
 */
public class MySegmentsUpdateWorker extends UpdateWorker {

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
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            mNotificationsQueue.take();
            mSynchronizer.forceMySegmentsSync();
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueuing polling task.");
        } catch (InterruptedException e) {
            Logger.d("My segments update worker has been interrupted");
            throw (e);
        }
    }
}
