package io.split.android.client.service.sseclient;

import com.google.gson.annotations.SerializedName;

public class SseAuthenticationResponse {

    private boolean isClientError = false;

    @SerializedName("pushEnabled")
    private boolean isStreamingEnabled;
    private String token;

    @SerializedName("connDelay")
    private Long sseConnectionDelay;

    public SseAuthenticationResponse() {
    }

    public SseAuthenticationResponse(boolean isClientError) {
        this.isClientError = isClientError;
    }

    public boolean isClientError() {
        return isClientError;
    }

    public boolean isStreamingEnabled() {
        return isStreamingEnabled;
    }

    public String getToken() {
        return token;
    }

    public Long getSseConnectionDelay() {
        return sseConnectionDelay;
    }
}
