package io.split.android.client.service.sseclient;

import java.util.List;

public class SseJwtToken {
    final private long expirationTime;
    final private List<String> channels;

    public SseJwtToken(long expirationTime, List<String> channels) {
        this.expirationTime = expirationTime;
        this.channels = channels;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public List<String> getChannels() {
        return channels;
    }
}
