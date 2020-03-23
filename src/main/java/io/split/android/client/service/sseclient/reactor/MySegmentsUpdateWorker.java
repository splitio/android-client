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
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class MySegmentsUpdateWorker implements SyncManagerFeedbackListener {

    /***
     * This class will be in charge of update my segments when a new notification arrived.
     * The idea to listen to the feedback channel is to avoid implementing a while true
     * loop to check the queue.
     * Task are enqueued to be executed serially. This way a worth scenario will be an
     * iteration in an empty queue
     */

    private final static String EXECUTION_QUEUE_NAME = "MY_SEGMENTS_UPDATE_QUEUE";

    private final PushNotificationQueue<MySegmentChangeNotification> mNotificationQueue;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final Synchronizer mSynchronizer;
    private final SplitTaskExecutor mSplitTaskExecutor;

    public MySegmentsUpdateWorker(
            @NonNull PushNotificationQueue<MySegmentChangeNotification> notificationQueue,
            @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel,
            @NonNull Synchronizer synchronizer,
            @NonNull SplitTaskExecutor spliTaskExecutor) {
        mNotificationQueue = checkNotNull(notificationQueue);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
        mSynchronizer = checkNotNull(synchronizer);
        mSplitTaskExecutor = checkNotNull(spliTaskExecutor);
    }

    @Override
    public void onFeedbackMessage(SyncManagerFeedbackMessage message) {
        // Here we execute the task in the same queue (thread) so that the execution is serial
        if (SyncManagerFeedbackMessageType.MY_SEGMENTS_UPDATED.equals(message.getMessage())) {
            mSplitTaskExecutor.execute(
                    new MySegmentsUpdaterTask(mNotificationQueue, mSynchronizer),
                    EXECUTION_QUEUE_NAME);
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueing polling task.");
        }
    }

    private class MySegmentsUpdaterTask implements SplitTask {
        private final PushNotificationQueue<MySegmentChangeNotification> mNotificationQueue;
        private final Synchronizer mSynchronizer;

        public MySegmentsUpdaterTask(
                PushNotificationQueue<MySegmentChangeNotification> notificationQueue,
                Synchronizer synchronizer) {
            mNotificationQueue = notificationQueue;
            mSynchronizer = synchronizer;
        }

        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            MySegmentChangeNotification notification = mNotificationQueue.get();
            while (notification != null) {
                mSynchronizer.syncronizeMySegments();
                notification = mNotificationQueue.get();
            }
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }
    }
}
