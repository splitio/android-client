package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.Nullable;

import io.split.android.client.service.sseclient.SseJwtToken;

public class SseAuthenticationResult {
    private boolean success;
    private boolean isErrorRecoverable;
    private boolean pushEnabled;
    private SseJwtToken jwtToken;

    public SseAuthenticationResult(boolean success, boolean isErrorRecoverable, boolean pushEnabled, SseJwtToken jwtToken) {
        this.success = success;
        this.isErrorRecoverable = isErrorRecoverable;
        this.pushEnabled = pushEnabled;
        this.jwtToken = jwtToken;
    }

    public SseAuthenticationResult(boolean success, boolean isErrorRecoverable) {
        this(success, isErrorRecoverable, false, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isErrorRecoverable() {
        return isErrorRecoverable;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    @Nullable
    public SseJwtToken getJwtToken() {
        return jwtToken;
    }
}
