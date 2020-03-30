package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.List;
import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.SSE_DOWN_NOTIFICATOR;

public class PushNotificationManager implements SplitTaskExecutionListener, SseClientListener{

    private final static String DATA_FIELD = "data";
    private final static int SSE_RECONNECT_TIME_IN_SECONDS = 70;

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final SplitTaskFactory mSplitTaskFactory;
    private final NotificationProcessor mNotificationProcessor;

    private String mSseDownNotificatorTaskId = null;


    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SplitTaskFactory splitTaskFactory,
                                   @NonNull NotificationProcessor notificationProcessor,
                                   @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel) {
        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
        mSseClient.setListener(this);
    }

    public void start() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);

        scheduleSseDownNotification();
    }

    public void stop() {
        mSseClient.disconnect();
    }

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void scheduleSseDownNotification() {
        if(mSseDownNotificatorTaskId != null) {
            mTaskExecutor.stopTask(mSseDownNotificatorTaskId);

        }
        mSseDownNotificatorTaskId = mTaskExecutor.schedule(
                new SseDownNotificator(),
                0, SSE_RECONNECT_TIME_IN_SECONDS,
                this);
    }

    private void notifyPushEnabled() {
        mSyncManagerFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));
    }

    private void notifyPushDisabled() {
        mSyncManagerFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));
    }

//
//     SSE client listener implementation
//
        @Override
        public void onOpen() {
            notifyPushEnabled();
        }

        @Override
        public void onMessage(Map<String, String> values) {
            String messageData = values.get(DATA_FIELD);
            if (messageData != null) {
                mNotificationProcessor.process(messageData);
            }
            scheduleSseDownNotification();
        }

    @Override
    public void onKeepAlive() {
        scheduleSseDownNotification();
    }

    @Override
        public void onError() {
            notifyPushDisabled();
        }

//
//      Split Task Executor Listener implementation
//
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            Pair<String, List<String>> unpackedResult = unpackResult(taskInfo);
            if (unpackedResult != null && unpackedResult.second.size() > 0) {
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
                Object channelsObject = taskInfo.getObjectValue(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM);
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

        private class SseDownNotificator implements SplitTask {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                mSyncManagerFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(
                                SyncManagerFeedbackMessageType.PUSH_DISABLED));
                return SplitTaskExecutionInfo.success(SSE_DOWN_NOTIFICATOR);
            }
        }
}
