package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.PRIVATE;

public class NewPushNotificationManager {

    private final static int POOL_SIZE = 1;
    private final static long AWAIT_SHUTDOWN_TIME = 5;
    private final ScheduledExecutorService mExecutor;
    private final PushManagerEventBroadcaster mBroadcasterChannel;
    private final SseAuthenticator mSseAuthenticator;
    private final NewSseClient mSseClient;
    private SseRefreshTokenTimer mRefreshTokenTimer;
    private SseDisconnectionTimer mDisconnectionTimer;


    private final static int AUTHENTICATING = 1;
    private final static int CONNECTING = 3;
    private final static int CONNECTED = 4;
    private final static int DISCONNECTED = 5;
    private final static int STOPPED = 6;
    private AtomicInteger mStatus;

    @VisibleForTesting(otherwise = PRIVATE)
    public NewPushNotificationManager(@NonNull PushManagerEventBroadcaster broadcasterChannel,
                                      @NonNull SseAuthenticator sseAuthenticator,
                                      @NonNull NewSseClient sseClient,
                                      @NonNull SseRefreshTokenTimer refreshTokenTimer,
                                      @NonNull SseDisconnectionTimer disconnectionTimer,
                                      @Nullable ScheduledExecutorService executor) {
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
        mSseAuthenticator = checkNotNull(sseAuthenticator);
        mSseClient = checkNotNull(sseClient);
        mRefreshTokenTimer = checkNotNull(refreshTokenTimer);
        mDisconnectionTimer = checkNotNull(disconnectionTimer);
        mStatus = new AtomicInteger(DISCONNECTED);

        if(executor != null) {
            mExecutor = executor;
        } else {
            mExecutor = buildExecutor();
        }
    }

    public void start() {
        Logger.d("Push notification manager started");
        connect();
    }

    public void pause() {
        Logger.d("Push notification manager paused");
        mDisconnectionTimer.schedule(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                mSseClient.disconnect();
                mRefreshTokenTimer.cancel();
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        });
    }

    public void resume() {
        Logger.d("Push notification manager resumed");
        mDisconnectionTimer.cancel();
        if(mStatus.get() == DISCONNECTED) {
            connect();
        }
    }

    public void stop() {
        Logger.d("Shutting down SSE client");
        shutdownAndAwaitTermination();
    }
    private void connect() {
        mExecutor.submit(new StreamingConnection());
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

        @Override
        public void run() {

            mStatus.set(AUTHENTICATING);
            SseAuthenticationResult authResult = mSseAuthenticator.authenticate();

            if(authResult.isSuccess() && !authResult.isPushEnabled()) {
                Logger.d("Streaming disabled for api key");
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_DISABLED));
                mStatus.set(STOPPED);
                return;
            }

            if(!authResult.isSuccess() && !authResult.isErrorRecoverable()) {
                Logger.d("Streaming no recoverable auth error.");
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_NON_RETRYABLE_ERROR));
                mStatus.set(STOPPED);
                return;
            }

            if( !authResult.isSuccess() && authResult.isErrorRecoverable()) {
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RETRYABLE_ERROR));
                mStatus.set(DISCONNECTED);
                return;
            }

            SseJwtToken token = authResult.getJwtToken();
            if(token == null || token.getChannels() == null || token.getRawJwt() == null) {
                Logger.d("Streaming auth error. Retrying");
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RETRYABLE_ERROR));
                mStatus.set(DISCONNECTED);
                return;
            }

            mStatus.set(CONNECTING);
            mSseClient.connect(token, new NewSseClientImpl.ConnectionListener() {
                @Override
                public void onConnectionSuccess() {
                    mStatus.set(CONNECTED);
                    mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));
                    mRefreshTokenTimer.schedule(token.getIssuedAtTime(), token.getExpirationTime());
                }
            });
        }

        private void logError(String message, Exception e) {
            Logger.e(message + " : " + e.getLocalizedMessage());
        }
    }
}


