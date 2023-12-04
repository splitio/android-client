package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

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
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class SseClientImpl implements SseClient {

    private final URI mTargetUrl;
    private final AtomicInteger mStatus;
    private final HttpClient mHttpClient;
    private final EventStreamParser mEventStreamParser;
    private final AtomicBoolean mIsDisconnectCalled;
    private final SseHandler mSseHandler;

    private final StringHelper mStringHelper;

    private HttpStreamRequest mHttpStreamRequest = null;

    private static final String PUSH_NOTIFICATION_CHANNELS_PARAM = "channel";
    private static final String PUSH_NOTIFICATION_TOKEN_PARAM = "accessToken";
    private static final String PUSH_NOTIFICATION_VERSION_PARAM = "v";
    private static final String PUSH_NOTIFICATION_VERSION_VALUE = "1.1";

    public SseClientImpl(@NonNull URI uri,
                         @NonNull HttpClient httpClient,
                         @NonNull EventStreamParser eventStreamParser,
                         @NonNull SseHandler sseHandler) {
        mTargetUrl = checkNotNull(uri);
        mHttpClient = checkNotNull(httpClient);
        mEventStreamParser = checkNotNull(eventStreamParser);
        mSseHandler = checkNotNull(sseHandler);
        mStatus = new AtomicInteger(DISCONNECTED);
        mIsDisconnectCalled = new AtomicBoolean(false);
        mStringHelper = new StringHelper();
        mStatus.set(DISCONNECTED);
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

    private void close() {
        Logger.d("Disconnecting SSE client");
        if (mStatus.getAndSet(DISCONNECTED) != DISCONNECTED) {
            if (mHttpStreamRequest != null) {
                mHttpStreamRequest.close();
            }
            Logger.d("SSE client disconnected");
        }
    }

    @Override
    public void connect(SseJwtToken token, ConnectionListener connectionListener) {
        mIsDisconnectCalled.set(false);
        mStatus.set(CONNECTING);
        boolean isConnectionConfirmed = false;
        String channels = mStringHelper.join(",", token.getChannels());
        String rawToken = token.getRawJwt();
        boolean isErrorRetryable = true;
        BufferedReader bufferedReader = null;
        try {
            URI url = new URIBuilder(mTargetUrl)
                    .addParameter(PUSH_NOTIFICATION_VERSION_PARAM, PUSH_NOTIFICATION_VERSION_VALUE)
                    .addParameter(PUSH_NOTIFICATION_CHANNELS_PARAM, channels)
                    .addParameter(PUSH_NOTIFICATION_TOKEN_PARAM, rawToken)
                    .build();
            mHttpStreamRequest = mHttpClient.streamRequest(url);
            HttpStreamResponse response = mHttpStreamRequest.execute();
            if (response.isSuccess()) {
                bufferedReader = response.getBufferedReader();
                if (bufferedReader != null) {
                    Logger.d("Streaming connection opened");
                    mStatus.set(CONNECTED);
                    String inputLine;
                    Map<String, String> values = new HashMap<>();
                    while ((inputLine = bufferedReader.readLine()) != null) {
                        if (mEventStreamParser.parseLineAndAppendValue(inputLine, values)) {
                            if (!isConnectionConfirmed) {
                                if (mEventStreamParser.isKeepAlive(values) || mSseHandler.isConnectionConfirmed(values)) {
                                    Logger.d("Streaming connection success");
                                    isConnectionConfirmed = true;
                                    connectionListener.onConnectionSuccess();
                                } else {
                                    Logger.d("Streaming error after connection");
                                    isErrorRetryable = mSseHandler.isRetryableError(values);
                                    break;
                                }
                            }
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
                isErrorRetryable = !response.isClientRelatedError();
            }
        } catch (URISyntaxException e) {
            logError("An error has occurred while creating stream Url ", e);
            isErrorRetryable = false;
        } catch (IOException e) {
            Logger.d("An error has occurred while parsing stream: ", e);
            isErrorRetryable = true;
        } catch (Exception e) {
            logError("An unexpected error has occurred while receiving stream events from: ", e);
            isErrorRetryable = true;
        } finally {
            if (!mIsDisconnectCalled.getAndSet(false)) {
                mSseHandler.handleError(isErrorRetryable);
                close();
            }
        }
    }

    private void logError(String message, Exception e) {
        Logger.e(message + " : " + e.getLocalizedMessage());
    }

}
