package io.split.android.client.service.sseclient.sseclient;

import io.split.android.client.service.sseclient.SseJwtToken;

public interface NewSseClient {

    final static int CONNECTING = 0;
    final static int DISCONNECTED = 2;
    final static int CONNECTED = 1;

    int status();

    void disconnect();

    void close();

    void connect(SseJwtToken token, ConnectionListener connectionListener);

    public static interface ConnectionListener {
        public void onConnectionSuccess();
    }
}
