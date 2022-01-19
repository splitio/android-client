package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.Nullable;

import io.split.android.client.service.sseclient.SseJwtToken;

public class SseAuthenticationResult {
    private boolean success;
    private boolean isErrorRecoverable;
    private boolean pushEnabled;
    private long sseConnectionDelay;
    private SseJwtToken jwtToken;
    @Nullable
    private final Integer httpStatus;

    public SseAuthenticationResult(boolean success, boolean isErrorRecoverable, boolean pushEnabled, long sseConnectionDelay, SseJwtToken jwtToken, @Nullable Integer httpStatus) {
        this.success = success;
        this.isErrorRecoverable = isErrorRecoverable;
        this.pushEnabled = pushEnabled;
        this.sseConnectionDelay = sseConnectionDelay;
        this.jwtToken = jwtToken;
        this.httpStatus = httpStatus;
    }

    public SseAuthenticationResult(boolean success, boolean isErrorRecoverable, boolean pushEnabled, long sseConnectionDelay, SseJwtToken jwtToken) {
        this(success, isErrorRecoverable, pushEnabled, sseConnectionDelay, jwtToken, null);
    }

    public SseAuthenticationResult(boolean success, boolean isErrorRecoverable) {
        this(success, isErrorRecoverable, false, 0, null);
    }

    public SseAuthenticationResult(int httpStatus) {
        this(false, true, false, 0, null, httpStatus);
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

    public long getSseConnectionDelay() {
        return sseConnectionDelay;
    }

    @Nullable
    public SseJwtToken getJwtToken() {
        return jwtToken;
    }

    @Nullable
    public Integer getHttpStatus() {
        return httpStatus;
    }
}
