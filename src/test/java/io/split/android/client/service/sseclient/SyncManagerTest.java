package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;

import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncManagerTest {


    @Mock
    SplitClientConfig mConfig;

    @Mock
    Synchronizer mSynchronizer;
    @Mock
    PushNotificationManager mPushNotificationManager;

    @Spy
    PushManagerEventBroadcaster mPushManagerEventBroadcaster;

    @Mock
    SplitUpdatesWorker mSplitsUpdateWorker;

    @Mock
    MySegmentsUpdateWorker mMySegmentUpdateWorker;


    SyncManager mSyncManager;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mSyncManager = new SyncManagerImpl(
                mConfig, mSynchronizer, mPushNotificationManager,
                mSplitsUpdateWorker, mMySegmentUpdateWorker, mPushManagerEventBroadcaster);
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
        verify(mPushManagerEventBroadcaster, times(1)).register((BroadcastedEventListener) mSyncManager);
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
        verify(mPushManagerEventBroadcaster, never()).register((BroadcastedEventListener) mSyncManager);
        verify(mPushNotificationManager, never()).start();
    }

    @Test
    public void disablePushNotificationReceived() {
        mSyncManager.start();
        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(ENABLE_POLLING));

        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, never()).stopPeriodicFetching();
    }

    @Test
    public void disableAndEnablePushNotificationReceived() {
        mSyncManager.start();
        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(ENABLE_POLLING));

        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(DISABLE_POLLING));

        verify(mSynchronizer, times(1)).stopPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicFetching();
    }
}
