package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static io.split.android.client.service.sseclient.notifications.NotificationType.CONTROL;
import static java.lang.reflect.Modifier.PRIVATE;

public class PushNotificationManager implements SplitTaskExecutionListener, SseClientListener {

    private final static String DATA_FIELD = "data";
    private final static int SSE_KEEPALIVE_TIME_IN_SECONDS = 70;
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    private final static String PRIMARY_CONTROL_CHANNEL = "control_pri";

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final SplitTaskFactory mSplitTaskFactory;
    private final NotificationParser mNotificationParser;
    private final NotificationProcessor mNotificationProcessor;
    private final ReconnectBackoffCounter mAuthBackoffCounter;
    private final ReconnectBackoffCounter mSseBackoffCounter;

    private String mResetSseKeepAliveTimerTaskId = null;
    private String mSseTokenExpiredTimerTaskId = null;
    private SseJwtToken mLastJwtTokenObtained = null;
    private AtomicBoolean mIsPollingEnabled;

    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull SplitTaskExecutor taskExecutor,
                                   @NonNull SplitTaskFactory splitTaskFactory,
                                   @NonNull NotificationParser notificationParser,
                                   @NonNull NotificationProcessor notificationProcessor,
                                   @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                   @NonNull ReconnectBackoffCounter authBackoffCounter,
                                   @NonNull ReconnectBackoffCounter sseBackoffCounter) {

        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mNotificationParser = checkNotNull(notificationParser);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);
        mAuthBackoffCounter = checkNotNull(authBackoffCounter);
        mSseBackoffCounter = checkNotNull(sseBackoffCounter);
        mIsPollingEnabled = new AtomicBoolean(false);
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

    @VisibleForTesting(otherwise = PRIVATE)
    public void notifyPollingDisabled() {
        if (mIsPollingEnabled.getAndSet(false)) {
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(DISABLE_POLLING));
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public void notifyPollingEnabled() {
        if (!mIsPollingEnabled.getAndSet(true)) {
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(ENABLE_POLLING));
        }
    }

    //
//     SSE client listener implementation
//
    @Override
    public void onOpen() {
        mSseBackoffCounter.resetCounter();
        notifyPollingDisabled();
        resetSseKeepAliveTimer();
    }

    @Override
    public void onMessage(Map<String, String> values) {
        String messageData = values.get(DATA_FIELD);
        if (messageData != null) {
            IncomingNotification incomingNotification
                    = mNotificationParser.parseIncoming(messageData);
            if (incomingNotification == null) {
                return;
            }

            if (CONTROL.equals(incomingNotification.getType())) {
                processControlNotification(incomingNotification);
            } else {
                mNotificationProcessor.process(incomingNotification);
            }
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
        notifyPollingEnabled();
        if (isRecoverable) {
            scheduleSseReconnection();
        }
    }

    private void processControlNotification(IncomingNotification incomingNotification) {

        // Using only primary control channel for now
        if (!PRIMARY_CONTROL_CHANNEL.equals(incomingNotification.getChannel())) {
            return;
        }

        try {
            ControlNotification controlNotification
                    = mNotificationParser.parseControl(incomingNotification.getJsonData());
            if (controlNotification.getMetrics().getPublishers() < 1) {
                notifyPollingEnabled();
            } else {
                notifyPollingDisabled();
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse control notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected erro while processing control notification: " +
                    e.getLocalizedMessage());
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
                    notifyPollingEnabled();
                    return;
                }

                if ((!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isApiKeyValid(taskInfo)) ||
                        SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                                && !isStreamingEnabled(taskInfo)) {
                    Logger.e("Couldn't connect to SSE server. " +
                            "API invalid or streaming is disabled.");
                    notifyPollingEnabled();
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
                    notifyPollingEnabled();
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
                    ENABLE_POLLING));
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