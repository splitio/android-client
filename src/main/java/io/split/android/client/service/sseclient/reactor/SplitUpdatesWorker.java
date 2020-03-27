package io.split.android.client.service.sseclient.reactor;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.Synchronizer;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

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
        waitForNotifications();
    }

    @Override
    protected void onWaitForNotificationLoop() {
        try {
            SplitsChangeNotification notification = mNotificationsQueue.take();
            mSynchronizer.synchronizeSplits(notification.getChangeNumber());
            Logger.d("A new notification to update splits has been received. " +
                    "Enqueing polling task.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}
