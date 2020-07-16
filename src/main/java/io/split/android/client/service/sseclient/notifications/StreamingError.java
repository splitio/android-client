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
}
