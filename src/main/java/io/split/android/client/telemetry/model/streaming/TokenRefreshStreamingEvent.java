package io.split.android.client.telemetry.model.streaming;

import io.split.android.client.telemetry.model.EventTypeEnum;

public class TokenRefreshStreamingEvent extends StreamingEvent {

    public TokenRefreshStreamingEvent(long tokenExpirationUTC, long timestamp) {
        super(EventTypeEnum.TOKEN_REFRESH, tokenExpirationUTC, timestamp);
    }
}
