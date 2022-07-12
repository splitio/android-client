package io.split.android.client.service.sseclient.reactor;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.utils.logger.Logger;

public class SplitUpdatesWorker extends UpdateWorker {

    /***
     * This class will be in charge of update splits when a new notification arrived.
     */

    private final BlockingQueue<SplitsChangeNotification> mNotificationsQueue;
    private final Synchronizer mSynchronizer;

    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer,
                              @NonNull BlockingQueue<SplitsChangeNotification> notificationsQueue) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
    }

    @Override
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            SplitsChangeNotification notification = mNotificationsQueue.take();
            mSynchronizer.synchronizeSplits(notification.getChangeNumber());
            Logger.d("A new notification to update splits has been received. " +
                    "Enqueuing polling task.");
        } catch (InterruptedException e) {
            Logger.d("Splits update worker has been interrupted");
            throw (e);
        }
    }
}
