package io.split.android.client.service.sseclient;

public interface SseConnectionManagerListener {
    void onSseAvailable();
    void onSseNotAvailable();
}
