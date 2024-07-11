package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;
import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.executor.ThreadFactoryBuilder;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.service.sseclient.feedbackchannel.DelayStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class PushNotificationManager {

    private final static int POOL_SIZE = 1;
    private final static long AWAIT_SHUTDOWN_TIME = 5;
    private final ScheduledExecutorService mExecutor;
    private final PushManagerEventBroadcaster mBroadcasterChannel;
    private final SseAuthenticator mSseAuthenticator;
    private final SseClient mSseClient;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final SseRefreshTokenTimer mRefreshTokenTimer;
    private final SseDisconnectionTimer mDisconnectionTimer;

    private final AtomicBoolean mIsPaused;
    private final AtomicBoolean mIsStopped;
    private Future<?> mConnectionTask;
    private final SplitTask mBackgroundDisconnectionTask;
    private final long mDefaultSSEConnectionDelayInSecs;

    public PushNotificationManager(@NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                   @NonNull SseAuthenticator sseAuthenticator,
                                   @NonNull SseClient sseClient,
                                   @NonNull SseRefreshTokenTimer refreshTokenTimer,
                                   @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                   long defaultSSEConnectionDelayInSecs,
                                   long sseDisconnectionDelayInSecs,
                                   @Nullable ScheduledExecutorService executorService) {
        this(pushManagerEventBroadcaster,
                sseAuthenticator,
                sseClient,
                refreshTokenTimer,
                new SseDisconnectionTimer(new SplitSingleThreadTaskExecutor(), Math.toIntExact(sseDisconnectionDelayInSecs)),
                telemetryRuntimeProducer,
                defaultSSEConnectionDelayInSecs,
                executorService);
    }

    @VisibleForTesting
    public PushNotificationManager(@NonNull PushManagerEventBroadcaster broadcasterChannel,
                                   @NonNull SseAuthenticator sseAuthenticator,
                                   @NonNull SseClient sseClient,
                                   @NonNull SseRefreshTokenTimer refreshTokenTimer,
                                   @NonNull SseDisconnectionTimer disconnectionTimer,
                                   @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                   long defaultSSEConnectionDelayInSecs,
                                   @Nullable ScheduledExecutorService executor) {
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
        mSseAuthenticator = checkNotNull(sseAuthenticator);
        mSseClient = checkNotNull(sseClient);
        mRefreshTokenTimer = checkNotNull(refreshTokenTimer);
        mDisconnectionTimer = checkNotNull(disconnectionTimer);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mIsStopped = new AtomicBoolean(false);
        mIsPaused = new AtomicBoolean(false);
        mBackgroundDisconnectionTask = new BackgroundDisconnectionTask(mSseClient, mRefreshTokenTimer);
        mDefaultSSEConnectionDelayInSecs = defaultSSEConnectionDelayInSecs;
        if (executor != null) {
            mExecutor = executor;
        } else {
            mExecutor = buildExecutor();
        }
    }

    public synchronized void start() {
        mTelemetryRuntimeProducer.recordStreamingEvents(new SyncModeUpdateStreamingEvent(SyncModeUpdateStreamingEvent.Mode.STREAMING, System.currentTimeMillis()));
        Logger.d("Push notification manager started");
        connect();
    }

    public void pause() {
        mIsPaused.set(true);
        mDisconnectionTimer.schedule(mBackgroundDisconnectionTask);
        Logger.d("Push notification manager paused");
    }

    public void resume() {
        if (!mIsPaused.compareAndSet(true, false)) {
            return;
        }
        mDisconnectionTimer.cancel();
        if (isSseClientDisconnected() && !mIsStopped.get()) {
            connect();
        }
        Logger.d("Push notification manager resumed");
    }

    public boolean isSseClientDisconnected() {
        return mSseClient.status() == SseClient.DISCONNECTED;
    }

    public synchronized void stop() {
        Logger.d("Shutting down SSE client");
        mIsStopped.set(true);
        disconnect();
        shutdownAndAwaitTermination();
    }

    public void disconnect() {
        Logger.d("Disconnecting down SSE client");
        mDisconnectionTimer.cancel();
        mRefreshTokenTimer.cancel();
        mSseClient.disconnect();
    }

    public void connect() {
        if (mSseClient.status() == SseClient.CONNECTED) {
            mSseClient.disconnect();
        }
        if (mConnectionTask != null && (!mConnectionTask.isDone() || !mConnectionTask.isCancelled())) {
            mConnectionTask.cancel(true);
        }
        mConnectionTask = mExecutor.submit(new StreamingConnection(mDefaultSSEConnectionDelayInSecs));
    }

    private void shutdownAndAwaitTermination() {
        mExecutor.shutdown();
        try {
            if (!mExecutor.awaitTermination(AWAIT_SHUTDOWN_TIME, TimeUnit.SECONDS)) {
                mExecutor.shutdownNow();
                if (!mExecutor.awaitTermination(AWAIT_SHUTDOWN_TIME, TimeUnit.SECONDS))
                    System.err.println("Sse client pool did not terminate");
            }
        } catch (InterruptedException ie) {
            mExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ScheduledThreadPoolExecutor buildExecutor() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat("split-sse_client-%d");
        threadFactoryBuilder.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.e(e, "Error in thread: %s", t.getName());
            }
        });

        return new ScheduledThreadPoolExecutor(POOL_SIZE, threadFactoryBuilder.build());
    }

    private class StreamingConnection implements Runnable {

        private final long mDefaultSSEConnectionDelayInSecs;

        public StreamingConnection(long defaultSseConnectionDelaySecs) {
            mDefaultSSEConnectionDelayInSecs = defaultSseConnectionDelaySecs;
        }

        @Override
        public void run() {

            long startTime = System.currentTimeMillis();
            SseAuthenticationResult authResult = mSseAuthenticator.authenticate(mDefaultSSEConnectionDelayInSecs);
            mTelemetryRuntimeProducer.recordSyncLatency(OperationType.TOKEN, System.currentTimeMillis() - startTime);

            if (authResult.isSuccess() && !authResult.isPushEnabled()) {
                handlePushDisabled();
                return;
            }

            if (!authResult.isSuccess() && !authResult.isErrorRecoverable()) {
                handleNonRetryableError(authResult);
                recordNonRetryableError(authResult);
                return;
            }

            if (!authResult.isSuccess() && authResult.isErrorRecoverable()) {
                handleRetryableError();
                return;
            }

            SseJwtToken token = authResult.getJwtToken();
            if (token == null || token.getChannels() == null || token.getRawJwt() == null) {
                handleAuthError();
                return;
            }

            recordSuccessfulSyncAndTokenRefreshes(token);

            long delay = authResult.getSseConnectionDelay();
            mBroadcasterChannel.pushMessage(new DelayStatusEvent(delay));
            // Delay returns false if some error occurs
            if (delay > 0 && !delay(delay)) {
                return;
            }

            // If host app is in bg or push manager stopped, abort the process
            if (mIsPaused.get() || mIsStopped.get()) {
                return;
            }

            mSseClient.connect(token, new SseClientImpl.ConnectionListener() {
                @Override
                public void onConnectionSuccess() {
                    mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));
                    mRefreshTokenTimer.schedule(token.getIssuedAtTime(), token.getExpirationTime());
                }
            });
        }

        private void recordSuccessfulSyncAndTokenRefreshes(SseJwtToken token) {
            mTelemetryRuntimeProducer.recordSuccessfulSync(OperationType.TOKEN, System.currentTimeMillis());
            mTelemetryRuntimeProducer.recordStreamingEvents(new TokenRefreshStreamingEvent(token.getExpirationTime(), System.currentTimeMillis()));
            mTelemetryRuntimeProducer.recordTokenRefreshes();
        }

        private void handlePushDisabled() {
            Logger.d("Streaming disabled");
            mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_DOWN));
            mIsStopped.set(true);
        }

        private void handleNonRetryableError(SseAuthenticationResult authResult) {
            Logger.d("Streaming no recoverable auth error.");
            mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_NON_RETRYABLE_ERROR));
            mIsStopped.set(true);
        }

        private void recordNonRetryableError(SseAuthenticationResult authResult) {
            mTelemetryRuntimeProducer.recordAuthRejections();
            if (authResult.getHttpStatus() != null) {
                mTelemetryRuntimeProducer.recordSyncError(OperationType.TOKEN, authResult.getHttpStatus());
            }
        }

        private void handleAuthError() {
            Logger.d("Streaming auth error. Retrying");
            handleRetryableError();
        }

        private void handleRetryableError() {
            mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RETRYABLE_ERROR));
        }

        private boolean delay(long seconds) {
            try {
                sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }

    public static class BackgroundDisconnectionTask implements SplitTask {

        private final SseClient mSseClient;
        private final SseRefreshTokenTimer mRefreshTokenTimer;

        @VisibleForTesting
        public BackgroundDisconnectionTask(SseClient sseClient, SseRefreshTokenTimer refreshTokenTimer) {
            mSseClient = sseClient;
            mRefreshTokenTimer = refreshTokenTimer;
        }

        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            Logger.d("Disconnecting streaming while in background");
            mSseClient.disconnect();
            mRefreshTokenTimer.cancel();
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        }
    }
}
