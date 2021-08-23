package io.split.android.client.service.sseclient;

import com.google.gson.annotations.SerializedName;

class SseAuthToken {
    @SerializedName("x-ably-capability")
    private final String channelList;

    @SerializedName("iat")
    private final long issuedAt;

    @SerializedName("exp")
    private final long expirationAt;

    @SerializedName("connDelay")
    private final long sseConnectionDelay;

    public SseAuthToken(String channelList, long issuedAt, long expirationAt, long sseConnectionDelay) {
        this.channelList = channelList;
        this.issuedAt = issuedAt;
        this.expirationAt = expirationAt;
        this.sseConnectionDelay = sseConnectionDelay;
    }

    public String getChannelList() {
        return channelList;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public long getExpirationAt() {
        return expirationAt;
    }

    public long getSseConnectionDelay() {
        return sseConnectionDelay;
    }
}