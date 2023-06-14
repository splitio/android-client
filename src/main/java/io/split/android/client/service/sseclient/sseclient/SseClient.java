package io.split.android.client.service.sseclient.sseclient;

import io.split.android.client.service.sseclient.SseJwtToken;

public interface SseClient {

    int CONNECTING = 0;
    int CONNECTED = 1;
    int DISCONNECTED = 2;

    int status();

    void disconnect();

    void connect(SseJwtToken token, ConnectionListener connectionListener);

    interface ConnectionListener {
        void onConnectionSuccess();
    }
}
