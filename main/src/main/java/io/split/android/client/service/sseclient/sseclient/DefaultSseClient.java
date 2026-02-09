package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.utils.logger.Logger;

/**
 * Split-specific SSE client adapter.
 * <p>
 * Builds the Split streaming URL from an {@link SseJwtToken}
 * (channels, access token, version) and delegates the actual
 * SSE connection to a generic {@link EventSourceClient}.
 * <p>
 * Incoming SSE events are routed through {@link SseHandler}
 * for Split notification processing.
 */
public class DefaultSseClient implements SseClient {

    private static final String PUSH_NOTIFICATION_CHANNELS_PARAM = "channel";
    private static final String PUSH_NOTIFICATION_TOKEN_PARAM = "accessToken";
    private static final String PUSH_NOTIFICATION_VERSION_PARAM = "v";
    private static final String PUSH_NOTIFICATION_VERSION_VALUE = "1.1";

    private final URI mTargetUrl;
    private final EventSourceClient mEventSourceClient;
    private final SseHandler mSseHandler;
    private final StringHelper mStringHelper;

    public DefaultSseClient(@NonNull URI uri,
                            @NonNull EventSourceClient eventSourceClient,
                            @NonNull SseHandler sseHandler) {
        mTargetUrl = checkNotNull(uri);
        mEventSourceClient = checkNotNull(eventSourceClient);
        mSseHandler = checkNotNull(sseHandler);
        mStringHelper = new StringHelper();
    }

    @Override
    public int status() {
        return mEventSourceClient.status();
    }

    @Override
    public void disconnect() {
        mEventSourceClient.disconnect();
    }

    @Override
    public void connect(SseJwtToken token, ConnectionListener connectionListener) {
        String channels = mStringHelper.join(",", token.getChannels());
        String rawToken = token.getRawJwt();

        try {
            URI url = new URIBuilder(mTargetUrl)
                    .addParameter(PUSH_NOTIFICATION_VERSION_PARAM, PUSH_NOTIFICATION_VERSION_VALUE)
                    .addParameter(PUSH_NOTIFICATION_CHANNELS_PARAM, channels)
                    .addParameter(PUSH_NOTIFICATION_TOKEN_PARAM, rawToken)
                    .build();

            mEventSourceClient.connect(url, new EventSourceClient.EventHandler() {
                private boolean isConnectionConfirmed = false;

                @Override
                public void onOpen() {
                    Logger.d("Streaming connection opened");
                }

                @Override
                public void onMessage(@NonNull Map<String, String> event) {
                    if (!isConnectionConfirmed) {
                        boolean isKeepAlive = EventStreamParser.KEEP_ALIVE_EVENT.equals(
                                event.get(EventStreamParser.EVENT_FIELD));
                        if (isKeepAlive || mSseHandler.isConnectionConfirmed(event)) {
                            Logger.d("Streaming connection success");
                            isConnectionConfirmed = true;
                            connectionListener.onConnectionSuccess();
                        } else {
                            Logger.d("Streaming error after connection");
                            boolean retryable = mSseHandler.isRetryableError(event);
                            mSseHandler.handleError(retryable);
                            mEventSourceClient.disconnect();
                            return;
                        }
                    }

                    boolean isKeepAlive = EventStreamParser.KEEP_ALIVE_EVENT.equals(
                            event.get(EventStreamParser.EVENT_FIELD));
                    if (!isKeepAlive) {
                        mSseHandler.handleIncomingMessage(event);
                    }
                }

                @Override
                public void onError(boolean retryable) {
                    mSseHandler.handleError(retryable);
                }
            });
        } catch (URISyntaxException e) {
            Logger.e("An error has occurred while creating stream URL: " + e.getLocalizedMessage());
            mSseHandler.handleError(false);
        }
    }
}
