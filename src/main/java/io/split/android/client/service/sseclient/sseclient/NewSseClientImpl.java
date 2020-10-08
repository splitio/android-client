package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.PRIVATE;

public class NewSseClientImpl implements NewSseClient {

    private final URI mTargetUrl;
    private AtomicInteger mStatus;
    private final HttpClient mHttpClient;
    private EventStreamParser mEventStreamParser;
    private AtomicBoolean isDisconnectCalled;
    private SseHandler mSseHandler;

    private final StringHelper mStringHelper;

    private BufferedReader mBufferedReader;
    private HttpStreamRequest mHttpStreamRequest = null;

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_VALUE_STREAM = "text/event-stream";
    private static final String PUSH_NOTIFICATION_CHANNELS_PARAM = "channel";
    private static final String PUSH_NOTIFICATION_TOKEN_PARAM = "accessToken";
    private static final String PUSH_NOTIFICATION_VERSION_PARAM = "v";
    private static final String PUSH_NOTIFICATION_VERSION_VALUE = "1.1";

    @VisibleForTesting(otherwise = PRIVATE)
    public NewSseClientImpl(@NonNull URI uri,
                            @NonNull HttpClient httpClient,
                            @NonNull EventStreamParser eventStreamParser,
                            @NonNull SseHandler sseHandler) {
        mTargetUrl = checkNotNull(uri);
        mHttpClient = checkNotNull(httpClient);
        mEventStreamParser = checkNotNull(eventStreamParser);
        mSseHandler = checkNotNull(sseHandler);
        mStatus = new AtomicInteger(DISCONNECTED);
        isDisconnectCalled = new AtomicBoolean(false);
        mStringHelper = new StringHelper();
        mStatus.set(DISCONNECTED);
    }

    public int status() {
        return mStatus.get();
    }

    @Override
    public void disconnect() {
        if (status() != DISCONNECTED) {
            isDisconnectCalled.set(true);
            setDisconnectedStatus();
            if (mHttpStreamRequest != null) {
                mHttpStreamRequest.close();
            }
        }
    }

    @Override
    public void close() {
        Logger.d("Closing SSE client");
        setDisconnectedStatus();
        disconnect();
    }

    private void setDisconnectedStatus() {
        mStatus.set(DISCONNECTED);
    }

    private void setStatus(int state) {
        mStatus.set(state);
    }

    @Override
    public void connect(SseJwtToken token, ConnectionListener connectionListener) {
        mStatus.set(CONNECTING);
        String channels = mStringHelper.join(",", token.getChannels());
        String rawToken = token.getRawJwt();

        mBufferedReader = null;
        try {
            URI url = new URIBuilder(mTargetUrl)
                    .addParameter(PUSH_NOTIFICATION_VERSION_PARAM, PUSH_NOTIFICATION_VERSION_VALUE)
                    .addParameter(PUSH_NOTIFICATION_CHANNELS_PARAM, channels)
                    .addParameter(PUSH_NOTIFICATION_TOKEN_PARAM, rawToken)
                    .build();
            mHttpStreamRequest = mHttpClient.streamRequest(url);
            mHttpStreamRequest.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE_STREAM);
            HttpStreamResponse response = mHttpStreamRequest.execute();
            if (response.isSuccess()) {
                mBufferedReader = response.getBufferedReader();
                if (mBufferedReader != null) {
                    Logger.i("Streaming connection opened");
                    mStatus.set(CONNECTED);
                    connectionListener.onConnectionSuccess();
                    String inputLine;
                    Map<String, String> values = new HashMap<>();
                    while ((inputLine = mBufferedReader.readLine()) != null) {
                        if (mEventStreamParser.parseLineAndAppendValue(inputLine, values)) {
                            // Keep alive has to be handled by connection timeout
                            if (!mEventStreamParser.isKeepAlive(values)) {
                                mSseHandler.handleIncomingMessage(values);
                            }
                            values = new HashMap<>();
                        }
                    }
                } else {
                    throw (new IOException("Buffer is null"));
                }
            } else {
                Logger.e("Streaming connection error. Http return code " + response.getHttpStatus());
                mSseHandler.reportError(!response.isClientRelatedError());
            }
        } catch (URISyntaxException e) {
            logError("An error has ocurred while creating stream Url ", e);
            mSseHandler.reportError(false);
        } catch (IOException e) {
            if (!isDisconnectCalled.getAndSet(false)) {
                logError("An error has ocurred while parsing stream from: ", e);
                mSseHandler.reportError(true);
            }
        } catch (Exception e) {
            logError("An unexpected error has ocurred while receiving stream events from: ", e);
            mSseHandler.reportError(true);
        } finally {
            close();
        }
    }

    private void logError(String message, Exception e) {
        Logger.e(message + " : " + e.getLocalizedMessage());
    }

}


