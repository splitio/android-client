package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static androidx.core.util.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.PRIVATE;

public class SseClient {

    private final static int POOL_SIZE = 4;
    private final static long AWAIT_SHUTDOWN_TIME = 5;
    private final URI mTargetUrl;
    private AtomicInteger mReadyState;
    private final HttpClient mHttpClient;
    private EventStreamParser mEventStreamParser;
    private WeakReference<SseClientListener> mListener;
    private final ScheduledExecutorService mExecutor;
    private ScheduledFuture mDisconnectionTimerTaskRef = null;
    private AtomicBoolean isDisconnectCalled;
    private Future mConnectionTask;
    private PersistentConnection mCurrentConnection;

    final static int CONNECTING = 0;
    final static int CLOSED = 2;
    final static int OPEN = 1;

    public SseClient(@NonNull URI uri,
                     @NonNull HttpClient httpClient,
                     @NonNull EventStreamParser eventStreamParser) {
        this(uri, httpClient, eventStreamParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public SseClient(@NonNull URI uri,
                     @NonNull HttpClient httpClient,
                     @NonNull EventStreamParser eventStreamParser,
                     @NonNull ScheduledExecutorService executor) {
        mTargetUrl = checkNotNull(uri);
        mHttpClient = checkNotNull(httpClient);
        mEventStreamParser = checkNotNull(eventStreamParser);
        mReadyState = new AtomicInteger(CLOSED);
        isDisconnectCalled = new AtomicBoolean(false);
        mExecutor = checkNotNull(executor);
        mReadyState.set(CLOSED);
    }

    public int readyState() {
        return mReadyState.get();
    }

    public void connect(String token, List<String> channels) {
        mReadyState.set(CONNECTING);
        mCurrentConnection = new PersistentConnection(token, channels);
        mConnectionTask = mExecutor.submit(mCurrentConnection);
    }

    public void disconnect() {
        if (readyState() != CLOSED) {
            isDisconnectCalled.set(true);
            if (mCurrentConnection != null) {
                mCurrentConnection.close();
            }
            if (mConnectionTask != null) {
                mConnectionTask.cancel(false);
            }
            setCloseStatus();
            triggerOnDisconnect();
        }
    }

    public void setListener(SseClientListener listener) {
        mListener = new WeakReference<>(listener);
    }

    public void close() {
        Logger.d("Shutting down SSE client");
        disconnect();
        shutdownAndAwaitTermination();
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

    private void triggerOnDisconnect() {
        SseClientListener listener = mListener.get();
        if (listener != null) {
            listener.onDisconnect();
        }
    }

    private void setCloseStatus() {
        mReadyState.set(CLOSED);
    }

    private void setReadyState(int state) {
        mReadyState.set(state);
    }

    public void scheduleDisconnection
            (long delayInSecods) {
        if (!mExecutor.isShutdown()) {
            Logger.d(String.format("Streaming will be disconnected in %d seconds", delayInSecods));
            cancelDisconnectionTimer();
            mDisconnectionTimerTaskRef = mExecutor.schedule(new DisconnectionTimer(), delayInSecods, TimeUnit.SECONDS);
        }
    }

    public boolean cancelDisconnectionTimer() {
        boolean taskWasCancelled = false;
        if (mDisconnectionTimerTaskRef != null) {
            mDisconnectionTimerTaskRef.cancel(false);
            taskWasCancelled = mDisconnectionTimerTaskRef.isCancelled();
            mDisconnectionTimerTaskRef = null;
        }
        return taskWasCancelled;
    }

    private class DisconnectionTimer implements Runnable {
        @Override
        public void run() {
            Logger.d("Disconnecting while in background");
            disconnect();
        }
    }

    private class PersistentConnection implements Runnable {
        private static final String CONTENT_TYPE_HEADER = "Content-Type";
        private static final String CONTENT_TYPE_VALUE_STREAM = "text/event-stream";
        private static final String PUSH_NOTIFICATION_CHANNELS_PARAM = "channel";
        private static final String PUSH_NOTIFICATION_TOKEN_PARAM = "accessToken";
        private static final String PUSH_NOTIFICATION_VERSION_PARAM = "v";
        private static final String PUSH_NOTIFICATION_VERSION_VALUE = "1.1";

        private final StringHelper mStringHelper;
        private final List<String> mChannels;
        private final String mToken;
        private BufferedReader mBufferedReader;
        private HttpStreamRequest mHttpStreamRequest = null;

        private final WeakReference<SseClient> mSseClientReference;

        public PersistentConnection(@NonNull String token, @NonNull List<String> channels) {

            mSseClientReference = new WeakReference<>(checkNotNull(SseClient.this));
            mToken = checkNotNull(token);
            mChannels = checkNotNull(channels);
            mStringHelper = new StringHelper();
        }

        @Override
        public void run() {
            String channels = mStringHelper.join(",", mChannels);
            mBufferedReader = null;
            try {
                URI url = new URIBuilder(mTargetUrl)
                        .addParameter(PUSH_NOTIFICATION_VERSION_PARAM, PUSH_NOTIFICATION_VERSION_VALUE)
                        .addParameter(PUSH_NOTIFICATION_CHANNELS_PARAM, channels)
                        .addParameter(PUSH_NOTIFICATION_TOKEN_PARAM, mToken)
                        .build();
                mHttpStreamRequest = mHttpClient.streamRequest(url);
                mHttpStreamRequest.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE_STREAM);
                HttpStreamResponse response = mHttpStreamRequest.execute();
                if (response.isSuccess()) {
                    mBufferedReader = response.getBufferedReader();
                    if (mBufferedReader != null) {
                        Logger.i("Streaming connection opened");
                        triggerOnOpen();
                        setSseState(OPEN);

                        String inputLine;
                        Map<String, String> values = new HashMap<>();
                        while ((inputLine = mBufferedReader.readLine()) != null) {
                            if (mEventStreamParser.parseLineAndAppendValue(inputLine, values)) {
                                if (mEventStreamParser.isKeepAlive(values)) {
                                    triggerOnKeepAlive();
                                } else {
                                    triggerOnMessage(values);
                                }
                                values = new HashMap<>();
                            }
                        }
                    } else {
                        throw (new IOException("Buffer is null"));
                    }
                } else {
                    Logger.e("Streaming connection error. Http return code "
                            + response.getHttpStatus());
                    triggerOnError(!response.isCredentialsError());
                }
            } catch (URISyntaxException e) {
                Logger.e("An error has ocurred while creating stream Url " +
                        mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                triggerOnError(false);
            } catch (IOException e) {
                if (!getAndSetIsDisconnectCalled(false)) {
                    Logger.e("An error has ocurred while parsing stream from " +
                            mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                    triggerOnError(true);
                }
            } catch (Exception e) {
                Logger.e("An unexpected error has ocurred while receiving stream events from " +
                        mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                triggerOnError(true);
            } finally {
                close();
            }
        }

        void triggerOnMessage(Map<String, String> messageValues) {
            if (mListener == null) {
                return;
            }
            SseClientListener listener = mListener.get();
            if (listener != null) {
                listener.onMessage(messageValues);
            }
        }

        void triggerOnKeepAlive() {
            SseClientListener listener = mListener.get();
            if (listener != null) {
                listener.onKeepAlive();
            }
        }

        void triggerOnError(boolean isRecoverable) {
            SseClientListener listener = mListener.get();
            if (listener != null) {
                listener.onError(isRecoverable);
            }
        }

        void triggerOnOpen() {
            SseClientListener listener = mListener.get();
            if (listener != null) {
                listener.onOpen();
            }
        }

        boolean getAndSetIsDisconnectCalled(boolean isCalled) {
            SseClient sseClient = mSseClientReference.get();
            if (sseClient != null) {
                return sseClient.isDisconnectCalled.getAndSet(isCalled);
            }
            return false;
        }

        public void close() {
            if (mHttpStreamRequest != null) {
                mHttpStreamRequest.close();
            }
            setSseState(CLOSED);
        }

        void setSseState(int state) {
            SseClient sseClient = mSseClientReference.get();
            if (sseClient != null) {
                sseClient.setReadyState(state);
            }
        }
    }
}


