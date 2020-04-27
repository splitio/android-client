package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.PUSH_DISABLED;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.PUSH_ENABLED;
import static java.lang.reflect.Modifier.PRIVATE;

public class PushNotificationManager implements SplitTaskExecutionListener, SseClientListener {

    private final static String DATA_FIELD = "data";
    private final static int SSE_KEEPALIVE_TIME_IN_SECONDS = 70;
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final SplitTaskFactory mSplitTaskFactory;
    private final NotificationProcessor mNotificationProcessor;
    private final ReconnectBackoffCounter mAuthBackoffCounter;
    private final ReconnectBackoffCounter mSseBackoffCounter;

    private String mResetSseKeepAliveTimerTaskId = null;
    private String mSseTokenExpiredTimerTaskId = null;
    private SseJwtToken mLastJwtTokenObtained = null;

    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SplitTaskFactory splitTaskFactory,
                                   @NonNull NotificationProcessor notificationProcessor,
                                   @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                   @NonNull ReconnectBackoffCounter authBackoffCounter,
                                   @NonNull ReconnectBackoffCounter sseBackoffCounter) {

        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);
        mAuthBackoffCounter = checkNotNull(authBackoffCounter);
        mSseBackoffCounter = checkNotNull(sseBackoffCounter);
        mSseClient.setListener(this);

    }

    public void start() {
        triggerSseAuthentication();
    }

    public void stop() {
        mSseClient.disconnect();
    }

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void scheduleReconnection() {
        mTaskExecutor.schedule(
                mSplitTaskFactory.createSseAuthenticationTask(),
                mAuthBackoffCounter.getNextRetryTime(), this);
    }

    private void scheduleSseReconnection() {
        mTaskExecutor.schedule(
                new SseReconnectionTimer(),
                mSseBackoffCounter.getNextRetryTime(), this);
    }

    private void triggerSseAuthentication() {
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);
    }


    private void resetSseKeepAliveTimer() {
        mResetSseKeepAliveTimerTaskId = mTaskExecutor.schedule(
                new SseKeepAliveTimer(),
                SSE_KEEPALIVE_TIME_IN_SECONDS,
                null);
    }

    private void resetSseTokenExpiredTimer(long expirationTime) {
        long reconnectTime
                = Math.max(expirationTime - RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS
                - System.currentTimeMillis() / 1000L, 0L);

        mSseTokenExpiredTimerTaskId = mTaskExecutor.schedule(
                new SseTokenExpiredTimer(),
                reconnectTime,
                null);
    }

    private void notifyPushEnabled() {
        mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(PUSH_ENABLED));
    }

    private void notifyPushDisabled() {
        mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(PUSH_DISABLED));
    }

    //
//     SSE client listener implementation
//
    @Override
    public void onOpen() {
        mSseBackoffCounter.resetCounter();
        notifyPushEnabled();
        resetSseKeepAliveTimer();
    }

    @Override
    public void onMessage(Map<String, String> values) {
        String messageData = values.get(DATA_FIELD);
        if (messageData != null) {
            mNotificationProcessor.process(messageData);
        }
        resetSseKeepAliveTimer();
    }

    @Override
    public void onKeepAlive() {
        resetSseKeepAliveTimer();
    }

    @Override
    public void onError(boolean isRecoverable) {
        cancelSseKeepAliveTimer();
        notifyPushDisabled();
        if (isRecoverable) {
            scheduleSseReconnection();
        }
    }

    private void cancelSseKeepAliveTimer() {
        if (mResetSseKeepAliveTimerTaskId != null) {
            mTaskExecutor.stopTask(mResetSseKeepAliveTimerTaskId);
        }
    }

    private void refreshSseToken() {
        cancelSseKeepAliveTimer();
        mSseClient.disconnect();
        triggerSseAuthentication();
    }

    //
//      Split Task Executor Listener implementation
//
    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {

        switch (taskInfo.getTaskType()) {
            case SSE_AUTHENTICATION_TASK:

                if (isUnexepectedError(taskInfo)) {
                    scheduleReconnection();
                    notifyPushDisabled();
                    return;
                }

                if ((!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isApiKeyValid(taskInfo))) {
                    Logger.e("Couldn't connect to SSE server. Invalid apikey ");
                    notifyPushDisabled();
                    return;
                }

                if (SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isStreamingEnabled(taskInfo)) {
                    Logger.e("Will not connect to SSE server. Streaming disabled.");
                    notifyPushDisabled();
                    return;
                }

                SseJwtToken jwtToken = unpackResult(taskInfo);
                if (jwtToken != null && jwtToken.getChannels().size() > 0) {
                    mAuthBackoffCounter.resetCounter();
                    storeJwt(jwtToken);
                    connectToSse(jwtToken.getRawJwt(), jwtToken.getChannels());
                    resetSseTokenExpiredTimer(jwtToken.getExpirationTime());
                } else {
                    scheduleReconnection();
                    notifyPushDisabled();
                }
                break;
            default:
                Logger.e("Push notification manager unknown task");
        }
    }

    private boolean isUnexepectedError(SplitTaskExecutionInfo taskInfo) {
        Boolean unexpectedErrorOcurred =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.UNEXPECTED_ERROR);
        return unexpectedErrorOcurred != null && unexpectedErrorOcurred.booleanValue();
    }

    private boolean isApiKeyValid(SplitTaskExecutionInfo taskInfo) {
        Boolean isApiKeyValid =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY);
        return isApiKeyValid != null && isApiKeyValid.booleanValue();
    }

    private boolean isStreamingEnabled(SplitTaskExecutionInfo taskInfo) {
        Boolean isStreamingEnabled =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED);
        return isStreamingEnabled != null && isStreamingEnabled.booleanValue();
    }

    synchronized private void storeJwt(SseJwtToken token) {
        mLastJwtTokenObtained = token;
    }

    synchronized private SseJwtToken getLastJwt() {
        return mLastJwtTokenObtained;
    }

    @Nullable
    private SseJwtToken unpackResult(SplitTaskExecutionInfo taskInfo) {


        Object token = taskInfo.getObjectValue(SplitTaskExecutionInfo.PARSED_SSE_JWT);
        if (token != null) {
            try {
                return (SseJwtToken) token;
            } catch (ClassCastException e) {
                Logger.e("Sse authentication error. JWT not valid: " +
                        e.getLocalizedMessage());
            }
        } else {
            Logger.e("Sse authentication error. Token not available.");
        }

        return null;
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseKeepAliveTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(
                    PUSH_DISABLED));
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseTokenExpiredTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            refreshSseToken();
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseReconnectionTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            SseJwtToken token = getLastJwt();
            connectToSse(token.getRawJwt(), token.getChannels());
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }
}