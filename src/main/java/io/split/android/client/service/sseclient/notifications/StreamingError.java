package io.split.android.client.service.sseclient.notifications;

public class StreamingError {
    final private String message;
    final private int code;
    final private int statusCode;

    public StreamingError(String message, int code, int statusCode) {
        this.message = message;
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean shouldBeIgnored() {
        return !(code >= 40000 && code <= 49999);
    }

    public boolean isRetryable() {
        return code >= 40140 &&  code <= 40149;
    }
}
