package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.synchronizer.SyncManager;

import static androidx.core.util.Preconditions.checkNotNull;

public class PushNotificationManager {

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final SplitTaskFactory mSplitTaskFactory;
    private final AuthTaskListener mAuthTaskListener;

    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SplitTaskFactory splitTaskFactory,
                                   @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel) {
        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
        mAuthTaskListener = new AuthTaskListener();
    }

    public void start() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                mAuthTaskListener);
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
