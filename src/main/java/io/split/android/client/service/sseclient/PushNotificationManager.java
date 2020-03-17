package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.List;
import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.utils.Logger;

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

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void notifyPushEnabled() {
        mSyncManagerFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));
    }

    private void notifyPushDisabled() {
        mSyncManagerFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));
    }

    private class SseMessageHandler implements SseClientListener {

        @Override
        public void onOpen() {
            notifyPushEnabled();
        }

        @Override
        public void onMessage(Map<String, String> values) {
            // TODO: Process message and notify channel
        }

        @Override
        public void onError() {
            notifyPushDisabled();
        }
    }

    private class AuthTaskListener implements SplitTaskExecutionListener {
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            Pair<String, List<String>> unpackedResult = unpackResult(taskInfo);
            if (unpackedResult != null) {
                connectToSse(unpackedResult.first, unpackedResult.second);
            } else {
                notifyPushDisabled();
            }
        }

        @Nullable
        private Pair<String, List<String>> unpackResult(SplitTaskExecutionInfo taskInfo) {
            if (!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())) {
                return null;
            }
            Boolean isStreamingEnabled = taskInfo.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED);
            if (isStreamingEnabled != null && isStreamingEnabled.booleanValue()) {
                String token = taskInfo.getStringValue(SplitTaskExecutionInfo.SSE_TOKEN);
                Object channelsObject = taskInfo.getObjectValue(SplitTaskExecutionInfo.SSE_TOKEN);
                if (token != null && channelsObject != null) {
                    try {
                        List<String> channels = (List<String>) channelsObject;
                        return new Pair(token, channels);
                    } catch (ClassCastException e) {
                        Logger.e("Sse authentication error. Channels not valid: " +
                                e.getLocalizedMessage());
                    }
                } else {
                    Logger.e("Sse authentication error. Token or Channels not available.");
                }
            } else {
                Logger.e("Couldn't connect to SSE server. Streaming is disabled.");
            }
            return null;
        }

    }

}
