package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.JsonSyntaxException;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.SseClient;
import io.split.android.client.service.sseclient.SseClientListener;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.STREAMING_CONNECTED;
import static java.lang.reflect.Modifier.PRIVATE;

public class PushNotificationManager implements SplitTaskExecutionListener, SseClientListener, SplitLifecycleAware {
    // TODO: Split this class in smaller pieces
    private final static String DATA_FIELD = "data";
    private final static int SSE_KEEPALIVE_TIME_IN_SECONDS = 70;
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    private final static int DISCONNECT_ON_BG_TIME_IN_SECONDS = 60;
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
    private AtomicLong mLastControlNotificationTime;
    private AtomicBoolean mIsStreamingPaused;
    private AtomicBoolean mIsHostAppInBackground;
    private AtomicBoolean mIsStopped;

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
        mLastControlNotificationTime = new AtomicLong(0L);
        mIsStreamingPaused = new AtomicBoolean(false);
        mIsHostAppInBackground = new AtomicBoolean(false);
        mIsStopped = new AtomicBoolean(false);
        mSseClient.setListener(this);
    }

    public void start() {
        triggerSseAuthentication();
    }

    public void stop() {
        mIsStopped.set(true);
        cancelRefreshTokenTimer();
        cancelSseKeepAliveTimer();
        mSseClient.close();
    }

    @Override
    public void pause() {
        if(mIsStopped.get()) {
            return;
        }
        mIsHostAppInBackground.set(true);
        mSseClient.scheduleDisconnection(DISCONNECT_ON_BG_TIME_IN_SECONDS);
    }

    @Override
    public void resume() {
        if(mIsStopped.get()) {
            return;
        }
        // Checking sse client status, cancel scheduled disconnect if necessary
        // and check if cancel was successful
        if(mSseClient.readyState() == SseClient.CLOSED ||
                (mSseClient.readyState() == SseClient.OPEN && !mSseClient.cancelDisconnectionTimer())) {
            triggerSseAuthentication();
        }
    }

    void connectToSse(String token, List<String> channels) {
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
                mSseBackoffCounter.getNextRetryTime(), null);
    }

    void triggerSseAuthentication() {
        Logger.d("Connecting to SSE server");
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);
    }

    private void resetSseKeepAliveTimer() {
        cancelSseKeepAliveTimer();
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
        Logger.i("Sending polling disabled message through event broadcaster.");
        mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(DISABLE_POLLING));
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public void notifyPollingEnabled() {
        if (!mIsPollingEnabled.getAndSet(true)) {
            Logger.i("Enabling polling");
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(ENABLE_POLLING));
        }
    }

    public void notifyStreamingConnected() {
        Logger.i("Disabling polling");
        mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(STREAMING_CONNECTED));
    }

    //
//     SSE client listener implementation
//
    @Override
    public void onOpen() {
        if(mIsStopped.get()) {
            return;
        }
        mSseBackoffCounter.resetCounter();
        notifyPollingDisabled();
        resetSseKeepAliveTimer();
        notifyStreamingConnected();
    }

    @Override
    public void onMessage(Map<String, String> values) {
        if(mIsStopped.get()) {
            return;
        }
        String messageData = values.get(DATA_FIELD);
        resetSseKeepAliveTimer();
        if (messageData != null) {
            IncomingNotification incomingNotification
                    = mNotificationParser.parseIncoming(messageData);
            if (incomingNotification == null) {
                return;
            }

            switch (incomingNotification.getType()) {
                case ERROR:
                    shutdownStreaming();
                    break;
                case CONTROL:
                    processControlNotification(incomingNotification);
                    break;
                case OCCUPANCY:
                    processOccupancyNotification(incomingNotification);
                    break;
                default:
                    if (!mIsStreamingPaused.get()) {
                        mNotificationProcessor.process(incomingNotification);
                    }
            }
        }

    }

    @Override
    public void onKeepAlive() {
        resetSseKeepAliveTimer();
    }

    @Override
    public void onError(boolean isRecoverable) {
        if(mIsStopped.get()) {
            return;
        }
        cancelSseKeepAliveTimer();
        if(mIsHostAppInBackground.get()) {
            mSseClient.cancelDisconnectionTimer();
            return;
        }
        notifyPollingEnabled();
        if (isRecoverable) {
            scheduleSseReconnection();
        }
    }

    @Override
    public void onDisconnect() {
        if(mIsStopped.get()) {
            return;
        }
        cancelSseKeepAliveTimer();
        cancelRefreshTokenTimer();
    }

    private void processControlNotification(IncomingNotification incomingNotification) {
        try {
            if (mLastControlNotificationTime.get() > incomingNotification.getTimestamp()) {
                return;
            }

            mLastControlNotificationTime.set(incomingNotification.getTimestamp());
            ControlNotification controlNotification
                    = mNotificationParser.parseControl(incomingNotification.getJsonData());
            switch (controlNotification.getControlType()) {
                case STREAMING_DISABLED:
                    mIsStreamingPaused.set(true);
                    shutdownStreaming();
                    break;
                case STREAMING_ENABLED:
                    mIsStreamingPaused.set(false);
                    if (mSseClient.readyState() == SseClient.CLOSED) {
                        triggerSseAuthentication();
                    }
                    notifyPollingDisabled();
                    break;
                case STREAMING_PAUSED:
                    mIsStreamingPaused.set(true);
                    notifyPollingEnabled();
                    break;
                default:
                    Logger.e("Unknown message received" + controlNotification.getControlType());
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse control notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing control notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void shutdownStreaming() {
        cancelRefreshTokenTimer();
        cancelSseKeepAliveTimer();
        mSseClient.disconnect();
        notifyPollingEnabled();
    }

    private void processOccupancyNotification(IncomingNotification incomingNotification) {

        // Using only primary control channel for now
        if (!incomingNotification.getChannel().contains(PRIMARY_CONTROL_CHANNEL)) {
            Logger.d("Ignoring occupancy notification in channel: "
                    + incomingNotification.getChannel());
            return;
        }

        try {
            OccupancyNotification occupancyNotification
                    = mNotificationParser.parseOccupancy(incomingNotification.getJsonData());
            if (occupancyNotification.getMetrics().getPublishers() < 1) {
                Logger.i("No publishers available for streaming channel: "
                        + incomingNotification.getChannel());
                notifyPollingEnabled();
            } else {
                notifyPollingDisabled();
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse occupancy notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing occupancy notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void cancelSseKeepAliveTimer() {
        if (mResetSseKeepAliveTimerTaskId != null) {
            mTaskExecutor.stopTask(mResetSseKeepAliveTimerTaskId);
            mResetSseKeepAliveTimerTaskId = null;
        }
    }

    private void cancelRefreshTokenTimer() {
        if (mSseTokenExpiredTimerTaskId != null) {
            mTaskExecutor.stopTask(mSseTokenExpiredTimerTaskId);
            mSseTokenExpiredTimerTaskId = null;
        }
    }

    void refreshSseToken() {
        cancelSseKeepAliveTimer();
        mSseClient.disconnect();
        triggerSseAuthentication();
    }

    //
//      Split Task Executor Listener implementation
//
    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if(mIsStopped.get()) {
            return;
        }

        switch (taskInfo.getTaskType()) {
            case SSE_AUTHENTICATION_TASK:

                if (isUnexepectedError(taskInfo)) {
                    scheduleReconnection();
                    notifyPollingEnabled();
                    return;
                }
                Logger.d("Streaming enabled: " + isStreamingEnabled(taskInfo));
                if ((!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isApiKeyValid(taskInfo))) {
                    Logger.e("Couldn't connect to SSE server. Invalid apikey ");
                    notifyPollingEnabled();
                    return;
                }

                if (SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isStreamingEnabled(taskInfo)) {
                    Logger.e("Will not connect to SSE server. Streaming disabled.");
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
                Logger.e("Push notification manager unknown task: "
                        + taskInfo.getTaskType());
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

    synchronized SseJwtToken getLastJwt() {
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
            triggerSseAuthentication();
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
            Logger.d("Refreshing sse token");
            refreshSseToken();
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseReconnectionTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            Logger.d("Reconnecting to SSE server");
            SseJwtToken token = getLastJwt();
            connectToSse(token.getRawJwt(), token.getChannels());
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }
}
