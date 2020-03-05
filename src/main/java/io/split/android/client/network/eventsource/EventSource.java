package io.split.android.client.network.eventsource;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class EventSource {

    private final URI mTargetUrl;
    private AtomicInteger mReadyState;
    private final HttpClient mHttpClient;
    private HttpStreamRequest mHttpStreamRequest = null;
    private EventSourceStreamParser mEventSourceStreamParser;
    private WeakReference<EventSourceListener> mListener;

    final static int CONNECTING = 0;
    final static int CLOSED = 2;
    final static int OPEN = 1;

    public EventSource(@NonNull URI uri,
                       @NonNull HttpClient httpClient,
                       @NonNull EventSourceStreamParser eventSourceStreamParser,
                       @NonNull EventSourceListener listener) {
        mTargetUrl = checkNotNull(uri);
        mHttpClient = checkNotNull(httpClient);
        mEventSourceStreamParser = checkNotNull(eventSourceStreamParser);

        mReadyState = new AtomicInteger(CLOSED);
        mListener = new WeakReference<>(checkNotNull(listener));
        connect();
    }

    public int readyState() {
        return mReadyState.get();
    }

    public String url() {
        return mTargetUrl.toString();
    }

    public void disconnect() {
        mHttpStreamRequest.close();
    }

    private void connect() {
        mHttpStreamRequest = mHttpClient.streamRequest(mTargetUrl);
        try {
            HttpStreamResponse response = mHttpStreamRequest.execute();
            if (response.isSuccess()) {
                BufferedReader bufferedReader = response.getBufferedReader();
                String inputLine;
                Map<String, String> values = new HashMap<>();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    // parseLineAndAppendValue returns true if an event has to be dispatched
                    if (mEventSourceStreamParser.parseLineAndAppendValue(inputLine, values)) {
                        triggerOnMessage(values);
                        values = new HashMap<>();
                    }

                }
                bufferedReader.close();
            }
        } catch (HttpException e) {
            Logger.e("An error has ocurred while trying to connecto to stream " +
                    mTargetUrl.toString() + " : " + e.getLocalizedMessage());
            triggerOnError();
        } catch (IOException e) {
            Logger.e("An error has ocurred while parsing stream from " +
                    mTargetUrl.toString() + " : " + e.getLocalizedMessage());
            triggerOnError();
        }
    }

    private void triggerOnMessage(Map<String, String> messageValues) {
        EventSourceListener listener = mListener.get();
        if (listener != null) {
            listener.onMessage(messageValues);
        }
    }

    private void triggerOnError() {
        EventSourceListener listener = mListener.get();
        if (listener != null) {
            listener.onError();
        }
    }

    private void triggerOnOpen() {
        EventSourceListener listener = mListener.get();
        if (listener != null) {
            listener.onOpen();
        }
    }


}
