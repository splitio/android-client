package io.split.android.client.service.sseclient;

import com.google.gson.annotations.SerializedName;

public class SseAuthenticationResponse {
    private boolean isValidApiKey = true;
    @SerializedName("pushEnabled")
    private boolean isStreamingEnabled;
    private String token;

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
