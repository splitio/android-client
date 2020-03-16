package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class SseClient {

    private final static int POOL_SIZE = 1;
    private final URI mTargetUrl;
    private AtomicInteger mReadyState;
    private final HttpClient mHttpClient;
    private HttpStreamRequest mHttpStreamRequest = null;
    private NotificationParser mNotificationParser;
    private WeakReference<SseClientListener> mListener;
    private final ExecutorService mExecutor;

    final static int CONNECTING = 0;
    final static int CLOSED = 2;
    final static int OPEN = 1;

    public SseClient(@NonNull URI uri,
                     @NonNull HttpClient httpClient,
                     @NonNull NotificationParser notificationParser,
                     @NonNull SseClientListener listener) {
        mTargetUrl = checkNotNull(uri);
        mHttpClient = checkNotNull(httpClient);
        mNotificationParser = checkNotNull(notificationParser);
        mReadyState = new AtomicInteger(CLOSED);
        mListener = new WeakReference<>(checkNotNull(listener));
        mExecutor = Executors.newFixedThreadPool(POOL_SIZE);
        mReadyState.set(CLOSED);
    }

    public int readyState() {
        return mReadyState.get();
    }

    public String url() {
        return mTargetUrl.toString();
    }

    public void connect() {
        mReadyState.set(CONNECTING);
        mExecutor.execute(new PersistentConnectionExecutor());
    }

    public void disconnect() {
        mHttpStreamRequest.close();
    }

    public void close() {
        mHttpStreamRequest.close();
        shutdownAndAwaitTermination();
    }

    private void shutdownAndAwaitTermination() {
        mExecutor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!mExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                mExecutor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!mExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            mExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void setCloseStatus() {
        mReadyState.set(CLOSED);
        triggerOnError();
    }

    private void triggerOnMessage(Map<String, String> messageValues) {
        SseClientListener listener = mListener.get();
        if (listener != null) {
            listener.onMessage(messageValues);
        }
    }

    private void triggerOnError() {
        SseClientListener listener = mListener.get();
        if (listener != null) {
            listener.onError();
        }
    }

    private void triggerOnOpen() {
        SseClientListener listener = mListener.get();
        if (listener != null) {
            listener.onOpen();
        }
    }

    private class PersistentConnectionExecutor implements Runnable {
        @Override
        public void run() {
            mHttpStreamRequest = mHttpClient.streamRequest(mTargetUrl);
            try {
                HttpStreamResponse response = mHttpStreamRequest.execute();
                if (response.isSuccess()) {
                    mReadyState.set(OPEN);
                    BufferedReader bufferedReader = response.getBufferedReader();
                    String inputLine;
                    Map<String, String> values = new HashMap<>();
                    while ((inputLine = bufferedReader.readLine()) != null) {
                        // parseLineAndAppendValue returns true if an event has to be dispatched
                        if (mNotificationParser.parseLineAndAppendValue(inputLine, values)) {
                            triggerOnMessage(values);
                            values = new HashMap<>();
                        }
                    }
                    bufferedReader.close();
                } else {
                    setCloseStatus();
                }
            } catch (HttpException e) {
                Logger.e("An error has ocurred while trying to connecting to stream " +
                        mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                setCloseStatus();
            } catch (IOException e) {
                Logger.e("An error has ocurred while parsing stream from " +
                        mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                setCloseStatus();
            } catch (Exception e) {
                Logger.e("An unexpected error has ocurred while receiving stream events from " +
                        mTargetUrl.toString() + " : " + e.getLocalizedMessage());
                setCloseStatus();
            }
        }
    }
}
