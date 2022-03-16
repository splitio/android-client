package io.split.android.client.service.sseclient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.SyncManagerImpl;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.telemetry.TelemetrySynchronizer;

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

    @Mock
    BackoffCounterTimer mBackoffTimer;

    @Mock
    TelemetrySynchronizer mTelemetrySynchronizer;


    SyncManager mSyncManager;


    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mSyncManager = new SyncManagerImpl(
                mConfig, mSynchronizer, mPushNotificationManager,
                mSplitsUpdateWorker, mPushManagerEventBroadcaster,
                mBackoffTimer, mTelemetrySynchronizer);

        ((MySegmentsUpdateWorkerRegistry) mSyncManager).registerMySegmentsUpdateWorker("user_key", mMySegmentUpdateWorker);
        when(mConfig.streamingEnabled()).thenReturn(true);

    }

    @Test
    public void pushNotificationSetupEnabled() {
        mSyncManager.start();
        verify(mSynchronizer, times(1)).loadAndSynchronizeSplits();
        verify(mSynchronizer, times(1)).loadMySegmentsFromCache();
        verify(mSynchronizer, times(1)).synchronizeMySegments();
        verify(mSynchronizer, never()).startPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicRecording();
        verify(mPushManagerEventBroadcaster, times(1)).register((BroadcastedEventListener) mSyncManager);
    }

    @Test
    public void pushNotificationSetupDisabled() {
        when(mConfig.streamingEnabled()).thenReturn(false);
        mSyncManager.start();
        verify(mSynchronizer, times(1)).loadAndSynchronizeSplits();
        verify(mSynchronizer, times(1)).loadMySegmentsFromCache();
        verify(mSynchronizer, times(1)).synchronizeMySegments();
        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicRecording();
        verify(mPushManagerEventBroadcaster, never()).register((BroadcastedEventListener) mSyncManager);
        verify(mPushNotificationManager, never()).start();
    }

    @Test
    public void disablePushNotificationReceived() {
        mSyncManager.start();
        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(EventType.PUSH_SUBSYSTEM_DOWN));

        verify(mSynchronizer, times(1)).startPeriodicFetching();
        verify(mSynchronizer, never()).stopPeriodicFetching();
    }

    @Test
    public void disableAndEnablePushNotificationReceived() {
        mSyncManager.start();
        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(EventType.PUSH_SUBSYSTEM_DOWN));

        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));

        verify(mSynchronizer, times(1)).stopPeriodicFetching();
        verify(mSynchronizer, times(1)).startPeriodicFetching();
    }

    @Test
    public void streamingConnected() {
        mSyncManager.start();
        reset(mSynchronizer);
        mPushManagerEventBroadcaster.pushMessage(
                new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));

        verify(mSynchronizer, times(1)).synchronizeSplits();
        verify(mSynchronizer, times(1)).synchronizeMySegments();
    }

    @Test
    public void stopCallsDestroyOnTelemetrySynchronizer() {
        mSyncManager.stop();

        verify(mTelemetrySynchronizer).destroy();
    }

    @Test
    public void pauseCallsFlushOnTelemetrySynchronizer() {
        mSyncManager.pause();

        verify(mTelemetrySynchronizer).flush();
    }

    @Test
    public void startCallsSynchronizeStatsOnTelemetryManager() {
        mSyncManager.start();

        verify(mTelemetrySynchronizer).synchronizeStats();
    }

    @Test
    public void flushCallsFlushOnTelemetrySynchronizer() {
        mSyncManager.flush();

        verify(mTelemetrySynchronizer).flush();
    }

    @Test
    public void startCallsStartOnAllSegmentsWorkers() {
        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        ((MySegmentsUpdateWorkerRegistry) mSyncManager).registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        mSyncManager.start();

        verify(mMySegmentUpdateWorker).start();
        verify(anyKeyUpdateWorker).start();
    }

    @Test
    public void stopCallsStopOnMyAllSegmentsWorkers() {
        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        ((MySegmentsUpdateWorkerRegistry) mSyncManager).registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        mSyncManager.stop();

        verify(mMySegmentUpdateWorker).stop();
        verify(anyKeyUpdateWorker).stop();
    }
}
