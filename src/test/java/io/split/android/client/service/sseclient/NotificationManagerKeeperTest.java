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
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.sseclient.NotificationManagerKeeper;
import io.split.android.client.telemetry.model.EventTypeEnum;
import io.split.android.client.telemetry.model.streaming.OccupancyPriStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

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

    @Mock
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mManagerKeeper = new NotificationManagerKeeper(mBroadcasterChannel, mTelemetryRuntimeProducer);
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
        Assert.assertEquals(messageCaptor.getValue().getMessage(), EventType.PUSH_SUBSYSTEM_DOWN);
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
        Assert.assertEquals(messageCaptor.getValue().getMessage(), EventType.PUSH_SUBSYSTEM_UP);
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
        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_RESUMED);
        mManagerKeeper.handleControlNotification(mControlNotification);

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), EventType.PUSH_SUBSYSTEM_UP);
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
        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_RESUMED);
        mManagerKeeper.handleControlNotification(mControlNotification);

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void pausedStreamingIsRecordedInTelemetry() {
        ArgumentCaptor<StreamingStatusStreamingEvent> argumentCaptor = ArgumentCaptor.forClass(StreamingStatusStreamingEvent.class);

        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_PAUSED);
        when(mControlNotification.getTimestamp()).thenReturn(20L);

        mManagerKeeper.handleControlNotification(mControlNotification);

        verify(mTelemetryRuntimeProducer).recordStreamingEvents(argumentCaptor.capture());
        Assert.assertEquals(StreamingStatusStreamingEvent.Status.PAUSED.getNumericValue(), argumentCaptor.getValue().getEventData().longValue());
        Assert.assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), argumentCaptor.getValue().getEventType());
        Assert.assertTrue(argumentCaptor.getValue().getTimestamp() > 0);
    }

    @Test
    public void enabledStreamingIsRecordedInTelemetry() {
        ArgumentCaptor<StreamingStatusStreamingEvent> argumentCaptor = ArgumentCaptor.forClass(StreamingStatusStreamingEvent.class);

        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_RESUMED);
        when(mControlNotification.getTimestamp()).thenReturn(20L);

        mManagerKeeper.handleControlNotification(mControlNotification);

        verify(mTelemetryRuntimeProducer).recordStreamingEvents(argumentCaptor.capture());
        Assert.assertEquals(StreamingStatusStreamingEvent.Status.ENABLED.getNumericValue(), argumentCaptor.getValue().getEventData().longValue());
        Assert.assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), argumentCaptor.getValue().getEventType());
        Assert.assertTrue(argumentCaptor.getValue().getTimestamp() > 0);
    }

    @Test
    public void disabledStreamingIsRecordedInTelemetry() {
        ArgumentCaptor<StreamingStatusStreamingEvent> argumentCaptor = ArgumentCaptor.forClass(StreamingStatusStreamingEvent.class);

        when(mControlNotification.getControlType()).thenReturn(ControlNotification.ControlType.STREAMING_DISABLED);
        when(mControlNotification.getTimestamp()).thenReturn(20L);

        mManagerKeeper.handleControlNotification(mControlNotification);

        verify(mTelemetryRuntimeProducer).recordStreamingEvents(argumentCaptor.capture());
        Assert.assertEquals(StreamingStatusStreamingEvent.Status.DISABLED.getNumericValue(), argumentCaptor.getValue().getEventData().longValue());
        Assert.assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), argumentCaptor.getValue().getEventType());
        Assert.assertTrue(argumentCaptor.getValue().getTimestamp() > 0);
    }

    @Test
    public void occupancyPriIsRecordedInTelemetry() {
        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_PRI_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(20L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(true);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(false);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        ArgumentCaptor<StreamingEvent> argumentCaptor = ArgumentCaptor.forClass(StreamingEvent.class);

        verify(mTelemetryRuntimeProducer).recordStreamingEvents(argumentCaptor.capture());
        Assert.assertTrue(argumentCaptor.getValue() instanceof OccupancyPriStreamingEvent);
    }

    @Test
    public void occupancySecIsRecordedInTelemetry() {
        when(mOccupancyNotification.getChannel()).thenReturn(CONTROL_SEC_CHANNEL);
        when(mOccupancyNotification.getTimestamp()).thenReturn(20L);
        when(mMetrics.getPublishers()).thenReturn(0);
        when(mOccupancyNotification.isControlPriChannel()).thenReturn(false);
        when(mOccupancyNotification.isControlSecChannel()).thenReturn(true);
        mManagerKeeper.handleOccupancyNotification(mOccupancyNotification);

        ArgumentCaptor<StreamingEvent> argumentCaptor = ArgumentCaptor.forClass(StreamingEvent.class);

        verify(mTelemetryRuntimeProducer).recordStreamingEvents(argumentCaptor.capture());
        Assert.assertTrue(argumentCaptor.getValue() instanceof OccupancySecStreamingEvent);
    }
}
