package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.notifications.RuleBasedSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.StreamingError;
import io.split.android.client.service.sseclient.spi.StreamingTelemetry;
import io.split.android.client.service.sseclient.spi.UpdateNotificationListener;
import io.split.android.client.service.sseclient.sseclient.NotificationManagerKeeper;
import io.split.android.client.service.sseclient.sseclient.SseHandler;


public class SseHandlerTest {

    SseHandler mSseHandler;

    @Mock
    NotificationParser mNotificationParser;

    @Mock
    NotificationManagerKeeper mManagerKeeper;

    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    UpdateNotificationListener mUpdateListener;

    @Mock
    StreamingTelemetry mTelemetryRuntimeProducer;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mSseHandler = new SseHandler(mNotificationParser, mUpdateListener, mManagerKeeper, mBroadcasterChannel, mTelemetryRuntimeProducer);
        when(mNotificationParser.isError(any())).thenReturn(false);
    }

    @Test
    public void incomingSplitUpdate() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.SPLIT_UPDATE, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(true);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener).onUpdateNotification(incomingNotification);
    }

    @Test
    public void incomingSplitKill() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.SPLIT_KILL, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(true);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener).onUpdateNotification(incomingNotification);
    }

    @Test
    public void incomingMembershipUpdate() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.MEMBERSHIPS_MS_UPDATE, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(true);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener).onUpdateNotification(incomingNotification);
    }

    @Test
    public void incomingLargeMembershipUpdate() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.MEMBERSHIPS_LS_UPDATE, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(true);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener).onUpdateNotification(incomingNotification);
    }

    @Test
    public void streamingPaused() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.MEMBERSHIPS_LS_UPDATE, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(false);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener, never()).onUpdateNotification(incomingNotification);
    }

    @Test
    public void incomingOccupancy() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.OCCUPANCY, "", "", 100);
        OccupancyNotification notification = new OccupancyNotification();

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseOccupancy(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mManagerKeeper).handleOccupancyNotification(notification);
    }

    @Test
    public void controlStreaming() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.CONTROL, "", "", 100);
        ControlNotification notification = new ControlNotification();

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseControl(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mManagerKeeper).handleControlNotification(notification);
    }

    @Test
    public void incomingLowRetryableSseError() {
        incomingRetryableSseErrorTest(40140);
    }

    @Test
    public void incomingHighRetryableSseError() {
        incomingRetryableSseErrorTest(40149);
    }

    public void incomingRetryableSseErrorTest(int code) {
        StreamingError notification = new StreamingError("msg", code, code);

        when(mNotificationParser.isError(any())).thenReturn(true);
        when(mNotificationParser.parseError(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), EventType.PUSH_RETRYABLE_ERROR);
    }

    @Test
    public void incomingLowNonRetryableSseError() {
        incomingNonRetryableSseErrorTest(40000);
    }

    @Test
    public void incomingHighNonRetryableSseError() {
        incomingNonRetryableSseErrorTest(49999);
    }

    public void incomingNonRetryableSseErrorTest(int code) {
        when(mNotificationParser.isError(any())).thenReturn(true);
        StreamingError notification = new StreamingError("msg", code, code);

        when(mNotificationParser.parseError(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), EventType.PUSH_NON_RETRYABLE_ERROR);
    }

    @Test
    public void incomingIgnorableSseErrorTest() {
        StreamingError notification = new StreamingError("msg", 50000, 50000);

        when(mNotificationParser.isError(any())).thenReturn(true);
        when(mNotificationParser.parseError(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void ablyErrorIsRecordedInTelemetry() {
        setupNotification();

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mTelemetryRuntimeProducer).recordAblyError(eq(40000), anyLong());
    }

    @Test
    public void sseRecoverableConnectionErrorIsRecordedInTelemetry() {
        mSseHandler.handleError(false);

        verify(mTelemetryRuntimeProducer).recordConnectionError(eq(false), anyLong());
    }

    @Test
    public void sseNonRecoverableConnectionErrorIsRecordedInTelemetry() {
        mSseHandler.handleError(true);

        verify(mTelemetryRuntimeProducer).recordConnectionError(eq(true), anyLong());
    }

    @Test
    public void incomingRuleBasedSegmentChange() {
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.RULE_BASED_SEGMENT_UPDATE, "", "", 100);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mManagerKeeper.isStreamingActive()).thenReturn(true);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mUpdateListener).onUpdateNotification(incomingNotification);
    }

    private void setupNotification() {
        when(mNotificationParser.isError(any())).thenReturn(true);
        int code = 40000;
        StreamingError notification = new StreamingError("msg", code, code);

        when(mNotificationParser.isError(anyMap())).thenReturn(true);
        when(mNotificationParser.parseError(anyString())).thenReturn(notification);
    }

    private Map<String, String> buildMessage(String data) {
        Map<String, String> values = new HashMap<>();
        values.put("name", "message");
        values.put("data", data);
        values.put("id", "thisidvalue");
        return values;
    }
}
