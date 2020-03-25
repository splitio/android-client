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
     * Task are enqueued to be executed serially by the synchronizer
     */

    private final Synchronizer mSynchronizer;

    public MySegmentsUpdateWorker(
            @NonNull Synchronizer synchronizer) {
        mSynchronizer = checkNotNull(synchronizer);
    }

    @Override
    public void onFeedbackMessage(SyncManagerFeedbackMessage message) {
        if (SyncManagerFeedbackMessageType.MY_SEGMENTS_UPDATED.equals(message.getMessage())) {
            mSynchronizer.syncronizeMySegments();
            Logger.d("A new notification to update segments has been received. " +
                    "Enqueing polling task.");
        }
    }
}
