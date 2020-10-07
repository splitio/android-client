package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.sseclient.NotificationManagerKeeper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationManagerKeeperTest {

    private static final String CONTROL_PRI_CHANNEL = "[?occupancy=metrics.publishers]control_pri";
    private static final String CONTROL_SEC_CHANNEL = "[?occupancy=metrics.publishers]control_sec";

    NotificationManagerKeeper mManagerKeeper;

    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    OccupancyNotification mOccupancyNotification;

    @Mock
    ControlNotification mControlNotification;

    @Mock
    OccupancyNotification.Metrics mMetrics;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mManagerKeeper = new NotificationManagerKeeper(mBroadcasterChannel);
        when(mOccupancyNotification.getMetrics()).thenReturn(mMetrics);
    }

    @Test
    public void noAvailablePublishers() {
        // Notification manager keeper start assuming one publisher in primary channel
        // Receiving 0 publishers in primary and having 0 in sec, should enable polling

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(100L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_SUBSYSTEM_DOWN);
    }

    @Test
    public void noAvailablePublishersOldTimestamp() {
        // Notification manager keeper start assuming one publisher in primary channel
        // Receiving 0 publishers in primary and having 0 in sec, should enable polling

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(0L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void noAvailablePublishersInPriButAvailableInSec() {
        // Notification manager keeper start assuming one publisher in primary channel
        // Here we disable enable secondary channel (publishers = 1 notification) then
        // Primary is disabled (publishers = 0).
        // No event should be sent through broadcaster channel

        OccupancyNotification n1 = Mockito.mock(OccupancyNotification.class);
        when(n1.getChannel()).thenReturn(CONTROL_SEC_CHANNEL);
        when(n1.getTimestamp()).thenReturn(0L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(n1.isControlPriChannel()).thenReturn(false);
        when(n1.isControlSecChannel()).thenReturn(true);

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(0L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void secondaryAvailableNotificationReceivedWhenNoPublishers() {
        // Notification manager keeper start assuming one publisher in primary channel
        // Receiving 0 publishers in primary and having 0 in sec to inform streaming down
        // Receiving 1 publisher in secondary channel must inform streaming up

        OccupancyNotification n1 = Mockito.mock(OccupancyNotification.class);
        when(n1.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(n1.getTimestamp()).thenReturn(10L);
        when(n1.getMetrics()).thenReturn(mMetrics);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(n1.isControlPriChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(n1);

        Mockito.reset(mBroadcasterChannel);

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_SEC_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(20L);
        when(mMetrics.getPublishers()).thenReturn(1);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(false);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_SUBSYSTEM_UP);
    }

    @Test
    public void secondaryAvailableNotificationReceivedWhenNoPublishersOldTimestamp() {
        // Notification manager keeper start assuming one publisher in primary channel
        // Receiving 0 publishers in primary and having 0 in sec to inform streaming down
        // Receiving 1 publisher in secondary with old timestamp channel must not inform streaming up

        OccupancyNotification n1 = Mockito.mock(OccupancyNotification.class);
        when(n1.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(n1.getTimestamp()).thenReturn(100L);
        when(n1.getMetrics()).thenReturn(mMetrics);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(n1.isControlPriChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(n1);

        Mockito.reset(mBroadcasterChannel);

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_SEC_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(100L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(false);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        Mockito.reset(mBroadcasterChannel);

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_SEC_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(10L);
        when(mMetrics.getPublishers()).thenReturn(1);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(false);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void incomingControlStreamingEnabled() {
        // Checking streaming enabled

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(20L);
        when(mMetrics.getPublishers()).thenReturn(1);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(false);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        when(mControlNotification.getTimestamp()).thenReturn(20L);
        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_ENABLED);
        mManagerKeeper.handleControlNotification(mControlNotification);

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_SUBSYSTEM_UP);
    }

    @Test
    public void incomingControlStreamingEnabledNoPublishers() {
        // Checking streaming enabled

        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(20L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(false);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        Mockito.reset(mBroadcasterChannel);

        when(mControlNotification.getTimestamp()).thenReturn(20L);
        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_ENABLED);
        mManagerKeeper.handleControlNotification(mControlNotification);

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

}
