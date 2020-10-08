package io.split.android.fake;

import java.util.concurrent.CountDownLatch;

import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.service.sseclient.sseclient.NewSseClient;

public class NewSseClientMock implements NewSseClient {

    public CountDownLatch mConnectLatch;

    @Override
    public void disconnect() {

    }

    @Override
    public void connect(SseJwtToken token, ConnectionListener connectionListener) {
        connectionListener.onConnectionSuccess();
        mConnectLatch.countDown();
    }
}
