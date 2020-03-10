package io.split.android.client.service.sseclient;

public class SseAuthenticationResponse {
    private final boolean isValidApiKey;
    private final boolean isStreamingEnabled;
    private final String token;

    public SseAuthenticationResponse(boolean isValidApiKey, boolean isStreamingEnabled, String token) {
        this.isValidApiKey = isValidApiKey;
        this.isStreamingEnabled = isStreamingEnabled;
        this.token = token;
    }

    public boolean isValidApiKey() {
        return isValidApiKey;
    }

    public boolean isStreamingEnabled() {
        return isStreamingEnabled;
    }

    public String getToken() {
        return token;
    }
}
