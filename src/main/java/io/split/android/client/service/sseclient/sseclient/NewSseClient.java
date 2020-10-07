package io.split.android.client.service.sseclient.sseclient;

import io.split.android.client.service.sseclient.SseJwtToken;

public interface NewSseClient {
    void disconnect();

    void close();

    void connect(SseJwtToken token, ConnectionListener connectionListener);

    public static interface ConnectionListener {
        public void onConnectionSuccess();
    }
}
