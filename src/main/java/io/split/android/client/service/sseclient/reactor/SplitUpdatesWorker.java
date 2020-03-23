package io.split.android.client.service.sseclient.reactor;

import androidx.annotation.NonNull;

import io.split.android.client.service.Synchronizer;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.storage.splits.SplitsStorage;
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
    private final static String EXECUTION_QUEUE_NAME = "SPLITS_UPDATE_QUEUE";
    private final PushNotificationQueue<SplitsChangeNotification> mNotificationQueue;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final Synchronizer mSynchronizer;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitsStorage mSplitStorage;

    public SplitUpdatesWorker(
            @NonNull PushNotificationQueue<SplitsChangeNotification> notificationQueue,
            @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel,
            @NonNull Synchronizer synchronizer,
            @NonNull SplitsStorage splitStorage,
            @NonNull SplitTaskExecutor spliTaskExecutor) {
        mNotificationQueue = checkNotNull(notificationQueue);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
        mSynchronizer = checkNotNull(synchronizer);
        mSplitTaskExecutor = checkNotNull(spliTaskExecutor);
        mSplitStorage = checkNotNull(splitStorage);
    }

    @Override
    public void onFeedbackMessage(SyncManagerFeedbackMessage message) {
        if (SyncManagerFeedbackMessageType.SPLITS_UPDATED.equals(message.getMessage())) {
            mSplitTaskExecutor.execute(
                    new SplitsUpdaterTask(mNotificationQueue, mSynchronizer, mSplitStorage),
                    EXECUTION_QUEUE_NAME);
            Logger.d("A new notification to update splits has been received. " +
                    "Enqueing polling task.");
        }
    }

    private class SplitsUpdaterTask implements SplitTask {
        private final SplitsStorage mSplitStorage;
        private final PushNotificationQueue<SplitsChangeNotification> mNotificationQueue;
        private final Synchronizer mSynchronizer;

        public SplitsUpdaterTask(
                PushNotificationQueue<SplitsChangeNotification> notificationQueue,
                Synchronizer synchronizer,
                SplitsStorage splitStorage) {
            mNotificationQueue = notificationQueue;
            mSynchronizer = synchronizer;
            mSplitStorage = splitStorage;
        }

        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            long changeNumber = mSplitStorage.getTill();
            SplitsChangeNotification notification = mNotificationQueue.get();
            if (notification != null) {
                long notificationChangeNumber = notification.getChangeNumber();
                if (changeNumber < notificationChangeNumber) {
                    mSynchronizer.synchronizeSplits();
                }
            }
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }
    }
}
