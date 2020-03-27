package io.split.android.client.service.sseclient.reactor;

import androidx.annotation.NonNull;

import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class SplitUpdatesWorker implements SyncManagerFeedbackListener {

    /***
     * This class will be in charge of update splits when a new notification arrived.
     * The idea to listen to the feedback channel is to avoid implementing a while true
     * loop to check the queue.
     * Task are enqueued to be executed serial. This way a worth scenario will be an
     * iteration in an empty queue
     */
    private final Synchronizer mSynchronizer;

    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer) {
        mSynchronizer = checkNotNull(synchronizer);
    }

    @Override
    public void onFeedbackMessage(SyncManagerFeedbackMessage message) {
        if (SyncManagerFeedbackMessageType.SPLITS_UPDATED.equals(message.getMessage())) {
            if (message.getData() == null) {
                Logger.d("Error on update message. Change number is null.");
                return;
            }

            Long changeNumber = null;
            try {
                changeNumber = (Long) (message.getData());
            } catch (ClassCastException e) {
                Logger.e("Invalid change number received by update worker: "
                        + message.getData());
                return;
            }

            if (changeNumber != null && changeNumber != 0) {
                mSynchronizer.synchronizeSplits(changeNumber);
            }
            Logger.d("A new notification to update splits has been received. " +
                    "Enqueing polling task.");
        }
    }
}
