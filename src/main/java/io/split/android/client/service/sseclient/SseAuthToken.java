package io.split.android.client.service.sseclient;

import com.google.gson.annotations.SerializedName;

class SseAuthToken {
    @SerializedName("x-ably-capability")
    private final String channelList;

    @SerializedName("iat")
    private final long issuedAt;

    @SerializedName("exp")
    private final long expirationAt;


    public SseAuthToken(String channelList, long issuedAt, long expirationAt) {
        this.channelList = channelList;
        this.issuedAt = issuedAt;
        this.expirationAt = expirationAt;
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

}