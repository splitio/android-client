package io.split.android.client.service.sseclient.reactor;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.utils.logger.Logger;

/**
 * This class will be in charge of update my segments when a new notification arrived.
 */
public class MySegmentsUpdateWorker extends UpdateWorker {

    private final MySegmentsSynchronizer mSynchronizer;
    private final BlockingQueue<Long> mNotificationsQueue;

    public MySegmentsUpdateWorker(
            @NonNull MySegmentsSynchronizer synchronizer,
            @NonNull BlockingQueue<Long> notificationsQueue) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
    }

    @Override
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            Long syncDelay = mNotificationsQueue.take();
            mSynchronizer.forceMySegmentsSync();// TODO syncDelay);
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueuing polling task.");
        } catch (InterruptedException e) {
            Logger.d("My segments update worker has been interrupted");
            throw (e);
        }
    }
}
