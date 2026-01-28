package io.split.android.client.service.sseclient;

import java.util.List;

public class SseJwtToken {
    final private long issuedAtTime;
    final private long expirationTime;
    final private List<String> channels;
    final private String rawJwt;

    public SseJwtToken(long issuedAtTime, long expirationTime, List<String> channels, String rawJwt) {
        this.issuedAtTime = issuedAtTime;
        this.expirationTime = expirationTime;
        this.channels = channels;
        this.rawJwt = rawJwt;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public long getIssuedAtTime() {
        return issuedAtTime;
    }

    public List<String> getChannels() {
        return channels;
    }

    public String getRawJwt() {
        return rawJwt;
    }
}
