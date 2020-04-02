package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.synchronizer.NewSyncManager;
import io.split.android.client.service.synchronizer.NewSyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewSyncManagerTest {


    @Mock
    SplitClientConfig mConfig;

    @Mock
    Synchronizer mSynchronizer;
    @Mock
    PushNotificationManager mPushNotificationManager;

    @Spy
    SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;

    NewSyncManager mSyncManager;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mSyncManager = new NewSyncManagerImpl(
                mConfig, mSynchronizer, mPushNotificationManager, mSyncManagerFeedbackChannel);
        when(mConfig.streamingEnabled()).thenReturn(true);

    }

    @Test
    public void pushNotificationSetupEnabled() {
        mSyncManager.start();
        verify(mSynchronizer, times(1)).loadSplitsFromCache();
        verify(mSynchronizer, times(1)).loadMySegmentsFromCache();
        verify(mSynchronizer, times(1)).syncronizeMySegments();
        verify(mSynchronizer, times(1)).synchronizeSplits();
        verify(mSynchronizer, times(1)).synchronizeSplits();
        verify(mSynchronizer, never()).startPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicRecording();
        verify(mSyncManagerFeedbackChannel, times(1)).register((SyncManagerFeedbackListener) mSyncManager);
        verify(mPushNotificationManager, times(1)).start();
    }

    @Test
    public void pushNotificationSetupDisabled() {
        when(mConfig.streamingEnabled()).thenReturn(false);
        mSyncManager.start();
        verify(mSynchronizer, times(1)).loadSplitsFromCache();
        verify(mSynchronizer, times(1)).loadMySegmentsFromCache();
        verify(mSynchronizer, never()).syncronizeMySegments();
        verify(mSynchronizer, never()).synchronizeSplits();
        verify(mSynchronizer, never()).synchronizeSplits();
        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicRecording();
        verify(mSyncManagerFeedbackChannel, never()).register((SyncManagerFeedbackListener) mSyncManager);
        verify(mPushNotificationManager, never()).start();
    }

    @Test
    public void disablePushNotificationReceived() {
        mSyncManager.start();
        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, never()).stopPeriodicFetching();
    }

    @Test
    public void disableAndEnablePushNotificationReceived() {
        mSyncManager.start();
        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        mSyncManagerFeedbackChannel.pushMessage(
                new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));

        verify(mSynchronizer, times(1)).stopPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicFetching();
    }
}
