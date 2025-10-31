package io.split.android.client.service.sseclient.sseclient;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PushNotificationManagerDeferredStartTaskTest {

    @Mock
    private PushNotificationManager mPushNotificationManager;
    private PushNotificationManagerDeferredStartTask mTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mTask = new PushNotificationManagerDeferredStartTask(mPushNotificationManager);
    }

    @Test
    public void executionCallsStartOnPushNotificationManager() {
        mTask.execute();

        verify(mPushNotificationManager).start();
    }
}
