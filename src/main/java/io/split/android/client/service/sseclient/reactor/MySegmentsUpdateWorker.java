package io.split.android.client.service.sseclient.reactor;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsDeferredSyncConfig;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.utils.logger.Logger;

/**
 * This class will be in charge of update my segments when a new notification arrived.
 */
public class MySegmentsUpdateWorker extends UpdateWorker {

    private final MySegmentsSynchronizer mSynchronizer;
    private final BlockingQueue<MySegmentsDeferredSyncConfig> mNotificationsQueue;

    public MySegmentsUpdateWorker(
            @NonNull MySegmentsSynchronizer synchronizer,
            @NonNull BlockingQueue<MySegmentsDeferredSyncConfig> notificationsQueue) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
    }

    @Override
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            long syncDelay = mNotificationsQueue.take().getSyncDelay();
            mSynchronizer.forceMySegmentsSync();
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueuing polling task.");
        } catch (InterruptedException e) {
            Logger.d("My segments update worker has been interrupted");
            throw (e);
        }
    }
}
