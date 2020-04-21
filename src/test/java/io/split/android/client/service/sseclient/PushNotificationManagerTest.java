package io.split.android.client.service.sseclient;

import org.junit.After;
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
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseauthentication.SseAuthenticationTask;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.utils.Json;

import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    SplitTaskExecutor mTaskExecutor;
    @Mock
    SplitTaskFactory mSplitTaskFactory;
    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    SseAuthenticationTask mSseAuthTask;

    @Mock
    NotificationParser mNotificationParser;

    @Mock
    NotificationProcessor mNotificationProcessor;

    @Mock
    ReconnectBackoffCounter mAuthBackoffCounter;

    @Mock
    ReconnectBackoffCounter mSseBackoffCounter;

    PushNotificationManager mPushManager;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        when(mAuthBackoffCounter.getNextRetryTime()).thenReturn(1L);
        when(mSseBackoffCounter.getNextRetryTime()).thenReturn(1L);
        mPushManager = new PushNotificationManager(mSseClient, mTaskExecutor,
                mSplitTaskFactory, mNotificationParser, mNotificationProcessor, mBroadcasterChannel,
                mAuthBackoffCounter, mSseBackoffCounter);
    }

    @Test
    public void authOkAndSubscritionToSse() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mPushManager.onOpen();

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(DISABLE_POLLING, messageCaptor.getValue().getMessage());
        verify(mAuthBackoffCounter, times(1)).resetCounter();
        verify(mSseBackoffCounter, times(1)).resetCounter();
    }

    @Test
    public void sseAuthCredentialsError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();

        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, false);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mTaskExecutor, never()).schedule(any(SseAuthenticationTask.class), reconnectTime.capture() , any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(any(), any());
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());
        verify(mAuthBackoffCounter, never()).getNextRetryTime();
    }

    @Test
    public void sseAuthUnexpectedError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.UNEXPECTED_ERROR, true);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mTaskExecutor, times(1)).schedule(any(SseAuthenticationTask.class), reconnectTime.capture() , any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(any(), any());
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());
        verify(mAuthBackoffCounter, times(1)).getNextRetryTime();
    }

    @Test
    public void sseAuthStreamingDisabled() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, true);
        respData.put(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, false);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mTaskExecutor, times(1)).schedule(any(SseAuthenticationTask.class), reconnectTime.capture() , any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(any(), any());
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());
        verify(mAuthBackoffCounter, times(1)).getNextRetryTime();
    }

    @Test
    public void authOkAndSubscritionToSseRecoverableError() {
        List<String> channels = dummyChannels();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mPushManager.onError(true);

        verify(mTaskExecutor, times(1)).schedule(
                any(PushNotificationManager.SseReconnectionTimer.class),
                anyLong(),
                any(PushNotificationManager.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        // Fist time connecting should not be called because keepalive timer is not set yet
        verify(mTaskExecutor, never()).stopTask(anyString());

        verify(mTaskExecutor, times(1)).schedule(
                any(PushNotificationManager.SseReconnectionTimer.class),
                anyLong(),
                any(PushNotificationManager.class));
        verify(mSseBackoffCounter, times(1)).getNextRetryTime();

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());

        verify(mAuthBackoffCounter, times(1)).resetCounter();

    }

    @Test
    public void recoverableSseErrorWhileConnected() {
        List<String> channels = dummyChannels();
        String keepAliveTaskId = "id1";
        when(mTaskExecutor.schedule(any(PushNotificationManager.SseKeepAliveTimer.class),
                anyLong(), isNull())).thenReturn(keepAliveTaskId);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mPushManager.onOpen();
        // Reset broadcaster channel to count only messages delivered after on error
        reset(mBroadcasterChannel);
        reset(mSseBackoffCounter);
        mPushManager.onError(true);

        verify(mTaskExecutor, times(1)).schedule(
                any(PushNotificationManager.SseReconnectionTimer.class),
                anyLong(),
                any(PushNotificationManager.class));
        verify(mSseBackoffCounter, times(1)).getNextRetryTime();
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        verify(mTaskExecutor, times(1)).stopTask(keepAliveTaskId);

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());

    }

    @Test
    public void authOkAndSubscritionToSseUnrecoverableError() {
        List<String> channels = dummyChannels();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mPushManager.onError(false);

        verify(mTaskExecutor, never()).schedule(
                any(PushNotificationManager.SseReconnectionTimer.class),
                anyLong(),
                any(PushNotificationManager.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        // Fist time connecting should not be called because keepalive timer is not set yet
        verify(mTaskExecutor, never()).stopTask(anyString());

        verify(mSseBackoffCounter, never()).getNextRetryTime();

        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());

        verify(mAuthBackoffCounter, times(1)).resetCounter();

    }

    @Test
    public void authOkAndChannelsError() {
        List<String> channels = new ArrayList<>();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(TOKEN, channels);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(ENABLE_POLLING, messageCaptor.getValue().getMessage());
    }

    @Test
    public void onMessageToProcess() {
        List<String> channels = new ArrayList<>();
        String data = "{}";
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.SPLIT_KILL,
                        "channel", "{}"));
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        reset(mTaskExecutor);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, times(1))
                .process(any(IncomingNotification.class));
        ArgumentCaptor<Long> downNotificationTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1))
                .schedule(any(PushNotificationManager.SseKeepAliveTimer.class),
                        downNotificationTime.capture(), isNull());
        verify(mTaskExecutor, times(1))
                .schedule(any(PushNotificationManager.SseKeepAliveTimer.class),
                        downNotificationTime.capture(), isNull());
        Assert.assertEquals(70L, downNotificationTime.getValue().longValue());
    }

    @Test
    public void onMessagePrimaryControl() {
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        String data = "{\"metrics\": {\"publishers\": 1}}";
        ControlNotification controlNotification = Json.fromJson(data, ControlNotification.class);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.CONTROL,
                        "control_pri", data));
        when(mNotificationParser.parseControl(anyString()))
                .thenReturn(controlNotification);
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        reset(mTaskExecutor);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        // Enable polling prior to disable it
        mPushManager.notifyPollingEnabled();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, times(0))
                .process(any(IncomingNotification.class));
        ArgumentCaptor<Long> downNotificationTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1))
                .schedule(any(PushNotificationManager.SseKeepAliveTimer.class),
                        downNotificationTime.capture(), isNull());
        verify(mTaskExecutor, times(1))
                .schedule(any(PushNotificationManager.SseKeepAliveTimer.class),
                        downNotificationTime.capture(), isNull());
        Assert.assertEquals(70L, downNotificationTime.getValue().longValue());
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(DISABLE_POLLING, messageCaptor.getValue().getMessage());
    }

    @Test
    public void onMessageNoPrimaryControl() {
        List<String> channels = new ArrayList<>();
        channels.add("dummychannel");
        String data = "{\"metrics\": {\"publishers\": 1}}";
        ControlNotification controlNotification = Json.fromJson(data, ControlNotification.class);
        when(mNotificationParser.parseIncoming(anyString()))
                .thenReturn(new IncomingNotification(NotificationType.CONTROL,
                        "control_sec", data));
        when(mNotificationParser.parseControl(anyString()))
                .thenReturn(controlNotification);
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();

        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        // Enable polling prior to disable it
        mPushManager.notifyPollingEnabled();
        reset(mBroadcasterChannel);
        mPushManager.onMessage(message(data));

        verify(mBroadcasterChannel, never()).pushMessage(any());
    }

    @Test
    public void onKeepAlive() {
        List<String> channels = new ArrayList<>();
        String data = "{}";

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        reset(mTaskExecutor);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mPushManager.onKeepAlive();

        ArgumentCaptor<Long> downNotificationTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).schedule(any(PushNotificationManager.SseKeepAliveTimer.class), downNotificationTime.capture(), any());
        Assert.assertEquals(70L, downNotificationTime.getValue().longValue());
    }

    @Test
    public void setupSseTokenExpirationTimerOnAuth() throws InterruptedException {
        List<String> channels = dummyChannels();
        String data = "{}";

        long expirationTime = System.currentTimeMillis() / 1000 + 603 ;

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        reset(mTaskExecutor);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true, expirationTime
                )));

        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<Long> expirationCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).schedule(any(PushNotificationManager.SseTokenExpiredTimer.class), expirationCaptor.capture(), any());
        Assert.assertEquals(3, expirationCaptor.getValue().longValue());
    }

    @Test
    public void refreshSseToken() throws InterruptedException {
        String keepAliveTaskId = "keepAliveTaskId";
        List<String> channels = dummyChannels();
        String data = "{}";
        mPushManager.start();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        when(mTaskExecutor.schedule(
                any(PushNotificationManager.SseKeepAliveTimer.class), anyLong(), any()))
                .thenReturn(keepAliveTaskId);


        PushNotificationManager.SseTokenExpiredTimer tokenExpiredTimerTask
                = mPushManager.new SseTokenExpiredTimer();

        mPushManager.onKeepAlive();
        tokenExpiredTimerTask.execute();
        verify(mTaskExecutor, times(1)).stopTask(keepAliveTaskId);
        verify(mSseClient, times(1)).disconnect();
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
    }

    @After
    public void teardDown() {
        reset();
    }

    private Map<String, Object> buildAuthMap(String token,
                                             List<String> channels,
                                             boolean isApiKeyValid,
                                             boolean isStreamingEnabled) {
        return buildAuthMap(token, channels, isApiKeyValid, isStreamingEnabled,
                9999999L);
    }


    private Map<String, Object> buildAuthMap(String token, List<String> channels,
                                             boolean isApiKeyValid, boolean isStreamingEnabled,
                                             long expirationTime) {
        Map<String, Object> data = new HashMap<>();
        SseJwtToken jwtToken = new SseJwtToken(expirationTime, channels, TOKEN);
        data.put(SplitTaskExecutionInfo.PARSED_SSE_JWT, jwtToken);
        data.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, isApiKeyValid);
        data.put(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, isStreamingEnabled);
        return data;
    }

    private List<String> dummyChannels() {
        List<String> channels = new ArrayList<>();
        channels.add("channel1");
        channels.add("channel2");
        return channels;
    }

    private Map<String, String> message(String data) {
        Map<String, String> values = new HashMap<>();
        values.put("data", data);
        return values;
    }
}
