package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.synchronizer.SyncManager;

import static androidx.core.util.Preconditions.checkNotNull;

public class PushNotificationManager {

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;

    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel) {
        mSseClient = checkNotNull(sseClient);
        mTaskExecutor = checkNotNull(taskExecutor);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
    }

    public void start() {
        // Submit auth task
    }

    public void stop() {
        mSseClient.disconnect();
    }

    private void connectToSse() {
        mSseClient.connect();
    }

    private class SseMessageHandler implements SseClientListener {

        @Override
        public void onOpen() {

        }

        @Override
        public void onMessage(Map<String, String> values) {

        }

        @Override
        public void onError() {

        }
    }

    private class AuthTaskListener implements SplitTaskExecutionListener {
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            if(SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())) {

            } else {

            }
        }
    }

}
