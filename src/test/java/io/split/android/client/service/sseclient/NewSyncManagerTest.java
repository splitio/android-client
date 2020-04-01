package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannelImpl;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.synchronizer.NewSyncManager;
import io.split.android.client.service.synchronizer.NewSyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NewSyncManagerTest {


    @Mock
    SplitClientConfig mConfig;

    @Mock
    Synchronizer mSynchronizer;
    @Mock
    PushNotificationManager mPushNotificationManager;

    SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;

    NewSyncManager mSyncManager;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mSyncManagerFeedbackChannel = new SyncManagerFeedbackChannelImpl();
        mSyncManager = new NewSyncManagerImpl(
                mConfig, mSynchronizer, mPushNotificationManager, mSyncManagerFeedbackChannel);

        mSyncManager.start();
    }

    @Test
    public void pushNotificationSetup() {
        verify(mSynchronizer, times(1)).doInitialLoadFromCache();
        verify(mSynchronizer, times(1)).startPeriodicRecording();
        verify(mPushNotificationManager, times(1)).start();
    }

    @Test
    public void disablePushNotificationReceived() {
        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, never()).stopPeriodicFetching();
    }

    @Test
    public void disableAndEnablePushNotificationReceived() {
        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));

        verify(mSynchronizer, times(1)).stopPeriodicFetching();
    }
}
