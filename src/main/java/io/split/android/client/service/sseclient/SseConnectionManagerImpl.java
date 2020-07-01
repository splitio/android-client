package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static java.lang.reflect.Modifier.PRIVATE;

public class SseConnectionManagerImpl implements SseConnectionManager, SseClientListener, SplitTaskExecutionListener {

    private final static int SSE_KEEPALIVE_TIME_IN_SECONDS = 70;
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    private final static int DISCONNECT_ON_BG_TIME_IN_SECONDS = 60;
    private final static String PRIMARY_CONTROL_CHANNEL = "control_pri";

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final ReconnectBackoffCounter mAuthBackoffCounter;
    private final ReconnectBackoffCounter mSseBackoffCounter;

    private String mResetSseKeepAliveTimerTaskId = null;
    private String mSseTokenExpiredTimerTaskId = null;
    private String mAuthReconnectionTimerTaskId = null;
    private String mSseReconnectionTimerTaskId = null;

    private SseJwtToken mLastJwtTokenObtained = null;
    private AtomicBoolean mIsHostAppInBackground;
    private AtomicBoolean mIsStopped;
    private WeakReference<SseConnectionManagerListener> mListenerRef;

    public SseConnectionManagerImpl(@NonNull SseClient sseClient,
                                    @NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull SplitTaskFactory splitTaskFactory,
                                    @NonNull ReconnectBackoffCounter authBackoffCounter,
                                    @NonNull ReconnectBackoffCounter sseBackoffCounter) {

        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mAuthBackoffCounter = checkNotNull(authBackoffCounter);
        mSseBackoffCounter = checkNotNull(sseBackoffCounter);
        mIsHostAppInBackground = new AtomicBoolean(false);
        mIsStopped = new AtomicBoolean(false);
        mSseClient.setListener(this);
    }

    @Override
    public void setListener(SseConnectionManagerListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    public void start() {
        triggerSseAuthentication();
    }

    @Override
    public void stop() {
        mIsStopped.set(true);
        cancelRefreshTokenTimer();
        cancelSseKeepAliveTimer();
        mSseClient.close();
    }

    @Override
    public void pause() {
        if (mIsStopped.get()) {
            return;
        }
        mIsHostAppInBackground.set(true);
        cancelAuthReconnectionTimer();
        cancelSseReconnectionTimer();
        mSseClient.scheduleDisconnection(DISCONNECT_ON_BG_TIME_IN_SECONDS);
    }

    @Override
    public void resume() {
        if (mIsStopped.get()) {
            return;
        }
        // Checking sse client status, cancel scheduled disconnect if necessary
        // and check if cancel was successful
        boolean isDisconnectionTimerCancelled = mSseClient.cancelDisconnectionTimer();
        if (mSseClient.readyState() == SseClient.CLOSED ||
                (mSseClient.readyState() == SseClient.OPEN && !isDisconnectionTimerCancelled)) {
            triggerSseAuthentication();
        }
    }

    private void notifySseAvailable() {
        SseConnectionManagerListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onSseAvailable();
        }
    }

    private void notifySseNotAvailable() {
        SseConnectionManagerListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onSseNotAvailable();
        }
    }

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void triggerSseAuthentication() {
        Logger.d("Connecting to SSE server");
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);
    }

    @Override
    public void onOpen() {
        if (mIsStopped.get()) {
            return;
        }
        cancelAuthReconnectionTimer();
        cancelSseReconnectionTimer();
        mSseBackoffCounter.resetCounter();
        resetSseKeepAliveTimer();
        notifySseAvailable();
    }

    @Override
    public void onMessage(Map<String, String> values) {
        if (mIsStopped.get()) {
            return;
        }
        resetSseKeepAliveTimer();
    }

    @Override
    public void onKeepAlive() {
        resetSseKeepAliveTimer();
    }

    @Override
    public void onError(boolean isRecoverable) {
        if (mIsStopped.get()) {
            return;
        }
        notifySseNotAvailable();

        cancelSseKeepAliveTimer();
        if (mIsHostAppInBackground.get()) {
            mSseClient.cancelDisconnectionTimer();
            return;
        }

        if (isRecoverable) {
            scheduleSseReconnection();
        }
    }

    @Override
    public void onDisconnect() {
        cancelSseKeepAliveTimer();
        cancelRefreshTokenTimer();
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

    private void cancelAuthReconnectionTimer() {
        if (mAuthReconnectionTimerTaskId != null) {
            mTaskExecutor.stopTask(mAuthReconnectionTimerTaskId);
            mAuthReconnectionTimerTaskId = null;
        }
    }

    private void cancelSseReconnectionTimer() {
        if (mSseReconnectionTimerTaskId != null) {
            mTaskExecutor.stopTask(mSseReconnectionTimerTaskId);
            mSseReconnectionTimerTaskId = null;
        }
    }

    private void refreshSseToken() {
        cancelSseKeepAliveTimer();
        mSseClient.disconnect();
        triggerSseAuthentication();
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

    private void scheduleReconnection() {
        cancelAuthReconnectionTimer();
        mAuthReconnectionTimerTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createSseAuthenticationTask(),
                mAuthBackoffCounter.getNextRetryTime(), this);
    }

    private void scheduleSseReconnection() {
        cancelSseReconnectionTimer();
        mSseReconnectionTimerTaskId = mTaskExecutor.schedule(
                new SseReconnectionTimer(),
                mSseBackoffCounter.getNextRetryTime(), null);
    }

    //
//      Split Task Executor Listener implementation
//
    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (mIsStopped.get()) {
            return;
        }

        switch (taskInfo.getTaskType()) {
            case SSE_AUTHENTICATION_TASK:

                if (isUnexepectedError(taskInfo)) {
                    scheduleReconnection();
                    notifySseNotAvailable();
                    return;
                }
                Logger.d("Streaming enabled: " + isStreamingEnabled(taskInfo));
                if ((!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isApiKeyValid(taskInfo))) {
                    Logger.e("Couldn't connect to SSE server. Invalid apikey ");
                    stop();
                    notifySseNotAvailable();
                    return;
                }

                if (SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isStreamingEnabled(taskInfo)) {
                    Logger.e("Will not connect to SSE server. Streaming disabled.");
                    stop();
                    notifySseNotAvailable();
                    return;
                }

                SseJwtToken jwtToken = unpackResult(taskInfo);
                if (jwtToken != null && jwtToken.getChannels().size() > 0) {
                    cancelAuthReconnectionTimer();
                    cancelSseReconnectionTimer();
                    mAuthBackoffCounter.resetCounter();
                    storeJwt(jwtToken);
                    connectToSse(jwtToken.getRawJwt(), jwtToken.getChannels());
                    resetSseTokenExpiredTimer(jwtToken.getExpirationTime());
                } else {
                    scheduleReconnection();
                    notifySseNotAvailable();
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
            triggerSseAuthentication();
            notifySseNotAvailable();
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
