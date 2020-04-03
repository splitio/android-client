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
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEvent;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventType;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class PushNotificationManager implements SplitTaskExecutionListener, SseClientListener{

    private final static String DATA_FIELD = "data";
    private final static int SSE_RECONNECT_TIME_IN_SECONDS = 70;

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final SplitTaskFactory mSplitTaskFactory;
    private final NotificationProcessor mNotificationProcessor;

    private String mReconnectTaskId = null;


    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SplitTaskFactory splitTaskFactory,
                                   @NonNull NotificationProcessor notificationProcessor,
                                   @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster) {
        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);
        mSseClient.setListener(this);
    }

    public void start() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);

        scheduleReconnection();
    }

    public void stop() {
        mSseClient.disconnect();
    }

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void scheduleReconnection() {
        if(mReconnectTaskId != null) {
            mTaskExecutor.stopTask(mReconnectTaskId);

        }
        mReconnectTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createSseAuthenticationTask(),
                0, SSE_RECONNECT_TIME_IN_SECONDS,
                this);
    }

    private void notifyPushEnabled() {
        mPushManagerEventBroadcaster.pushMessage(new BroadcastedEvent(BroadcastedEventType.PUSH_ENABLED));
    }

    private void notifyPushDisabled() {
        mPushManagerEventBroadcaster.pushMessage(new BroadcastedEvent(BroadcastedEventType.PUSH_DISABLED));
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
            scheduleReconnection();
        }

    @Override
    public void onKeepAlive() {
        scheduleReconnection();
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
}
