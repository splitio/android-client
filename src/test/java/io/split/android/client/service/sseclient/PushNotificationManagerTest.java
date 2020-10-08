package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.notifications.StreamingMessageParser;
import io.split.android.client.utils.Json;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushNotificationManagerTest {

    private static final String TOKEN = "THETOKEN";

    @Mock
    SplitClientConfig mSplitClientConfig;

    @Mock
    SseClient mSseClient;

    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    SseConnectionManager mSseConnectionManager;

    @Mock
    NotificationParser mNotificationParser;

    @Mock
    StreamingMessageParser mStreamingMessageParser;

    @Mock
    NotificationProcessor mNotificationProcessor;



    PushNotificationManager mPushManager;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        mPushManager = new PushNotificationManager(mSseClient, mNotificationParser,
                 mNotificationProcessor, mStreamingMessageParser, mBroadcasterChannel, mSseConnectionManager);
    }


    @Test
    public void onMessageToProcess() {
        List<String> channels = new ArrayList<>();
        String data = "{}";
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.SPLIT_KILL,
                        "channel", "{}", 1L));

        mPushManager.start();

        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, times(1))
                .process(any(IncomingNotification.class));
    }

    @Test
    public void onMessagePrimaryOccupancy() {
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        String data = "{\"metrics\": {\"publishers\": 1}}";
        OccupancyNotification occupancyNotification = Json.fromJson(data, OccupancyNotification.class);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.OCCUPANCY,
                        "control_pri", data, 1L));
        when(mNotificationParser.parseOccupancy(anyString()))
                .thenReturn(occupancyNotification);

        mPushManager.start();

        // Enable polling prior to disable it
        mPushManager.notifyPollingEnabled();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, times(0))
                .process(any(IncomingNotification.class));
        ArgumentCaptor<Long> downNotificationTime = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(EventType.DISABLE_POLLING, messageCaptor.getValue().getMessage());
    }

    @Test
    public void onMessageNoPrimaryOccupancy() {
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        String data = "{\"metrics\": {\"publishers\": 1}}";
        OccupancyNotification occupancyNotification = Json.fromJson(data, OccupancyNotification.class);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.CONTROL,
                        "control_sec", data, 1L));
        when(mNotificationParser.parseOccupancy(anyString()))
                .thenReturn(occupancyNotification);

        mPushManager.start();

        // Enable polling prior to disable it
        mPushManager.notifyPollingEnabled();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(data));

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void onMessageError() {
        genericTestOnMessage(new IncomingNotification(NotificationType.ERROR,
                "", "", 1L));
    }

    @Test
    public void onMessageStreamingDisabled() {
        genericTestOnMessage(new IncomingNotification(NotificationType.CONTROL,
                "control_pri", "", 100L));
    }

    private void genericTestOnMessage(IncomingNotification incomingNotification) {
        ControlNotification streamingDisabledNotification
                = Json.fromJson("{\"type\":\"CONTROL\",\"controlType\":\"STREAMING_DISABLED\"}", ControlNotification.class);
        String keepAliveTaskId = "id1";
        String refreshTokenTaskId = "id2";
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(incomingNotification);
        when(mNotificationParser.parseControl(anyString()))
                .thenReturn(streamingDisabledNotification);
        ;

        mPushManager.start();
        mPushManager.onSseAvailable();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(""));

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(EventType.ENABLE_POLLING, messageCaptor.getValue().getMessage());
    }

    @Test
    public void onMessageStreamingPaused() {

        IncomingNotification incomingNotification = new IncomingNotification(NotificationType.CONTROL,
                "control_pri", "", 100L);
        ControlNotification streamingPausedNotification
                = Json.fromJson("{\"type\":\"CONTROL\",\"controlType\":\"STREAMING_PAUSED\"}", ControlNotification.class);
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(incomingNotification);
        when(mNotificationParser.parseControl(anyString()))
                .thenReturn(streamingPausedNotification);

        mPushManager.start();
        mPushManager.onSseAvailable();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(""));

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(EventType.ENABLE_POLLING, messageCaptor.getValue().getMessage());
    }

    @Test
    public void noProccessNotificationWhenPaused() {

        IncomingNotification incomingNotification = new IncomingNotification(NotificationType.CONTROL,
                "control_pri", "", 100L);
        ControlNotification streamingDisabledNotification
                = Json.fromJson("{\"type\":\"CONTROL\",\"controlType\":\"STREAMING_PAUSED\"}", ControlNotification.class);
        String keepAliveTaskId = "id1";
        String refreshTokenTaskId = "id2";
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        String data = "{\"metrics\": {\"publishers\": 1}}";
        OccupancyNotification occupancyNotification = Json.fromJson(data, OccupancyNotification.class);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(incomingNotification);
        when(mNotificationParser.parseControl(anyString()))
                .thenReturn(streamingDisabledNotification);

        mPushManager.start();
        mPushManager.onSseAvailable();
        mPushManager.onMessage(message(data));

        reset(mNotificationParser);
        IncomingNotification splitKillNot = new IncomingNotification(NotificationType.SPLIT_KILL,
                "control_pri", "", 100L);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(splitKillNot);

        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, never()).process(any());
    }

    private Map<String, String> message(String data) {
        Map<String, String> values = new HashMap<>();
        values.put("data", data);
        return values;
    }
}
