package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.spi.StreamingTransport;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingConnection;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingResponse;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingTransportException;
import io.split.android.client.utils.logger.Logger;

/**
 * Generic SSE client implementation.
 * <p>
 * Connects to an SSE endpoint using a {@link StreamingTransport},
 * parses the event stream with {@link EventStreamParser}, and
 * delivers raw events through an {@link EventHandler}.
 */
public class EventSourceClientImpl implements EventSourceClient {

    private final AtomicInteger mStatus;
    private final StreamingTransport mStreamingTransport;
    private final EventStreamParser mEventStreamParser;
    private final AtomicBoolean mIsDisconnectCalled;

    @Nullable
    private volatile StreamingConnection mStreamingConnection;
    @Nullable
    private volatile StreamingResponse mStreamingResponse;

    public EventSourceClientImpl(@NonNull StreamingTransport streamingTransport,
                                 @NonNull EventStreamParser eventStreamParser) {
        mStreamingTransport = Objects.requireNonNull(streamingTransport);
        mEventStreamParser = Objects.requireNonNull(eventStreamParser);
        mStatus = new AtomicInteger(DISCONNECTED);
        mIsDisconnectCalled = new AtomicBoolean(false);
    }

    @Override
    public int status() {
        return mStatus.get();
    }

    @Override
    public void disconnect() {
        if (!mIsDisconnectCalled.getAndSet(true)) {
            close();
        }
    }

    @Override
    public void connect(@NonNull URI url, @NonNull EventHandler handler) {
        mIsDisconnectCalled.set(false);
        mStatus.set(CONNECTING);
        boolean isErrorRetryable = true;
        BufferedReader bufferedReader = null;
        try {
            mStreamingConnection = mStreamingTransport.connect(url);
            mStreamingResponse = mStreamingConnection.execute();
            if (mStreamingResponse.isSuccess()) {
                bufferedReader = mStreamingResponse.getBufferedReader();
                if (bufferedReader != null) {
                    Logger.d("SSE connection opened");
                    mStatus.set(CONNECTED);
                    handler.onOpen();
                    String inputLine;
                    Map<String, String> values = new HashMap<>();
                    while ((inputLine = bufferedReader.readLine()) != null) {
                        if (mEventStreamParser.parseLineAndAppendValue(inputLine, values)) {
                            handler.onMessage(values);
                            values = new HashMap<>();
                        }
                    }
                } else {
                    throw new IOException("Buffer is null");
                }
            } else {
                Logger.e("SSE connection error. Http return code " + mStreamingResponse.getHttpStatus());
                isErrorRetryable = !mStreamingResponse.isClientRelatedError();
            }
        } catch (StreamingTransportException e) {
            logError("An error has occurred during SSE transport", e);
            isErrorRetryable = !isNotRetryableStatusCode(e.getStatusCode());
        } catch (IOException e) {
            Logger.d("SSE stream read error: " + e.getLocalizedMessage());
            isErrorRetryable = true;
        } catch (Exception e) {
            logError("An unexpected error has occurred during SSE connection", e);
            isErrorRetryable = true;
        } finally {
            if (!mIsDisconnectCalled.getAndSet(false)) {
                handler.onError(isErrorRetryable);
            }
            close();
        }
    }

    private void close() {
        Logger.d("Closing SSE connection");
        if (mStatus.getAndSet(DISCONNECTED) != DISCONNECTED) {
            if (mStreamingResponse != null) {
                try {
                    mStreamingResponse.close();
                    Logger.v("StreamingResponse closed successfully");
                } catch (IOException e) {
                    Logger.w("Failed to close StreamingResponse: " + e.getMessage());
                }
                mStreamingResponse = null;
            }

            if (mStreamingConnection != null) {
                mStreamingConnection.close();
                mStreamingConnection = null;
            }
            Logger.d("SSE connection closed");
        }
    }

    private boolean isNotRetryableStatusCode(@Nullable Integer statusCode) {
        if (statusCode == null) {
            return false;
        }
        return statusCode >= 400 && statusCode < 500 && statusCode != 408;
    }

    private static void logError(String message, Exception e) {
        Logger.e(message + " : " + e.getLocalizedMessage());
    }
}
