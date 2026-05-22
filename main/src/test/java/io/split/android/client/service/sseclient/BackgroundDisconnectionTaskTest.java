package io.split.android.client.service.sseclient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.sseclient.sseclient.SseRefreshTokenTimer;

public class BackgroundDisconnectionTaskTest {

    private SseClient mSseClient;
    private SseRefreshTokenTimer mTimer;
    private PushNotificationManager.BackgroundDisconnectionTask mTask;

    @Before
    public void setUp() {
        mSseClient = mock(SseClient.class);
        mTimer = mock(SseRefreshTokenTimer.class);
        mTask = new PushNotificationManager.BackgroundDisconnectionTask(mSseClient, mTimer);
    }

    @Test
    public void executionDisconnectsClientAndCancelsTimer() {
        mTask.run();

        verify(mSseClient).disconnect();
        verify(mTimer).cancel();
    }
}
