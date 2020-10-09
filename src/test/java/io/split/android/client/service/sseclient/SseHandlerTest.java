package io.split.android.client.service.sseclient;

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
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.StreamingError;
import io.split.android.client.service.sseclient.sseclient.NotificationManagerKeeper;
import io.split.android.client.service.sseclient.sseclient.SseHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SseHandlerTest {

    SseHandler mSseHandler;

    @Mock
    NotificationParser mNotificationParser;

    @Mock
    NotificationManagerKeeper mManagerKeeper;

    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mSseHandler = new SseHandler(mNotificationParser, mNotificationProcessor, mManagerKeeper, mBroadcasterChannel);
        when(mNotificationParser.isError(any())).thenReturn(false);
    }

    @Test
    public void incomingSplitUpdate() {


        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.SPLIT_UPDATE, "", "", 100);
        SplitsChangeNotification notification = new SplitsChangeNotification(-1);

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitUpdate(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mNotificationProcessor).process(incomingNotification);
    }

    @Test
    public void incomingSplitKill() {

        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.SPLIT_KILL, "", "", 100);
        SplitKillNotification notification = new SplitKillNotification();

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitKill(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mNotificationProcessor).process(incomingNotification);
    }

    @Test
    public void incomingMySegmentsUpdate() {

        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.MY_SEGMENTS_UPDATE, "", "", 100);
        MySegmentChangeNotification notification = new MySegmentChangeNotification();

        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mNotificationProcessor).process(incomingNotification);
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
        IncomingNotification incomingNotification =
                new IncomingNotification(NotificationType.ERROR, "", "", 100);
        StreamingError notification = new StreamingError("msg", 50000, 50000);


        when(mNotificationParser.isError(any())).thenReturn(true);
        when(mNotificationParser.parseError(anyString())).thenReturn(notification);

        mSseHandler.handleIncomingMessage(buildMessage("{}"));

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    private Map<String, String> buildMessage(String data) {
        Map<String, String> values = new HashMap<>();
        values.put("name", "message");
        values.put("data", data);
        values.put("id", "thisidvalue");
        return values;
    }
}
