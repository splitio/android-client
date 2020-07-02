package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.utils.Json;

import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SseConnectionManagerTest {

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
    SseConnectionManagerListener mSseConnectionManagerListener;

    @Mock
    SseAuthenticationTask mSseAuthTask;

    @Mock
    ReconnectBackoffCounter mAuthBackoffCounter;

    @Mock
    ReconnectBackoffCounter mSseBackoffCounter;

    SseConnectionManagerImpl mSseConnectionManager;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        when(mAuthBackoffCounter.getNextRetryTime()).thenReturn(1L);
        when(mSseBackoffCounter.getNextRetryTime()).thenReturn(1L);
        mSseConnectionManager = new SseConnectionManagerImpl(mSseClient, mTaskExecutor,
                mSplitTaskFactory, mAuthBackoffCounter, mSseBackoffCounter);

        mSseConnectionManager.setListener(mSseConnectionManagerListener);
    }

    @Test
    public void authOkAndSubscritionToSse() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mSseConnectionManager.onOpen();

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        verify(mSseConnectionManagerListener, times(1)).onSseAvailable();
        verify(mAuthBackoffCounter, times(1)).resetCounter();
        verify(mSseBackoffCounter, times(1)).resetCounter();
    }

    @Test
    public void sseAuthCredentialsError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();

        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, false);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
        verify(mTaskExecutor, never()).schedule(any(SseAuthenticationTask.class), reconnectTime.capture(), any(SseConnectionManagerImpl.class));
        verify(mSseClient, never()).connect(any(), any());
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
        verify(mAuthBackoffCounter, never()).getNextRetryTime();
    }

    @Test
    public void sseAuthUnexpectedError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.UNEXPECTED_ERROR, true);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
        verify(mTaskExecutor, times(1)).schedule(any(SseAuthenticationTask.class), reconnectTime.capture(), any(SseConnectionManagerImpl.class));
        verify(mSseClient, never()).connect(any(), any());
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
        verify(mAuthBackoffCounter, times(1)).getNextRetryTime();
    }

    @Test
    public void sseAuthStreamingDisabled() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        Map<String, Object> respData = new HashMap<>();
        respData.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, true);
        respData.put(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, false);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK, respData));

        ArgumentCaptor<Long> reconnectTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
        verify(mTaskExecutor, times(1)).schedule(any(SseAuthenticationTask.class), reconnectTime.capture(), any(SseConnectionManagerImpl.class));
        verify(mSseClient, never()).connect(any(), any());
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
        verify(mAuthBackoffCounter, times(1)).getNextRetryTime();
    }

    @Test
    public void authOkAndSubscritionToSseRecoverableError() {
        List<String> channels = dummyChannels();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mSseConnectionManager.onError(true);

        verify(mTaskExecutor, times(1)).schedule(
                any(SseConnectionManagerImpl.SseReconnectionTimer.class),
                anyLong(),
                isNull());
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        // Fist time connecting should not be called because keepalive timer is not set yet
        verify(mTaskExecutor, never()).stopTask(anyString());

        verify(mSseBackoffCounter, times(1)).getNextRetryTime();

        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();

        verify(mAuthBackoffCounter, times(1)).resetCounter();

    }

    @Test
    public void recoverableSseErrorWhileConnected() {
        List<String> channels = dummyChannels();
        String keepAliveTaskId = "id1";
        when(mTaskExecutor.schedule(any(SseConnectionManagerImpl.SseKeepAliveTimer.class),
                anyLong(), isNull())).thenReturn(keepAliveTaskId);
        mSseConnectionManager.start();
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mSseConnectionManager.onOpen();
        // Reset broadcaster channel to count only messages delivered after on error
        reset(mSseConnectionManagerListener);
        reset(mSseBackoffCounter);
        mSseConnectionManager.onError(true);

        verify(mTaskExecutor, times(1)).schedule(
                any(SseConnectionManagerImpl.SseReconnectionTimer.class),
                anyLong(),
                isNull());
        verify(mSseBackoffCounter, times(1)).getNextRetryTime();
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        verify(mTaskExecutor, times(1)).stopTask(keepAliveTaskId);
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
    }

    @Test
    public void authOkAndSubscritionToSseUnrecoverableError() {
        List<String> channels = dummyChannels();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mSseConnectionManager.onError(false);

        verify(mTaskExecutor, never()).schedule(
                any(SseConnectionManagerImpl.SseReconnectionTimer.class),
                anyLong(),
                any(SseConnectionManagerImpl.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<String> keepAliveId = ArgumentCaptor.forClass(String.class);
        // Fist time connecting should not be called because keepalive timer is not set yet
        verify(mTaskExecutor, never()).stopTask(anyString());
        verify(mSseBackoffCounter, never()).getNextRetryTime();
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
        verify(mAuthBackoffCounter, times(1)).resetCounter();
    }

    @Test
    public void authOkAndChannelsError() {
        List<String> channels = new ArrayList<>();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
        verify(mSseClient, never()).connect(TOKEN, channels);
        verify(mSseConnectionManagerListener, times(1)).onSseNotAvailable();
    }

    @Test
    public void onKeepAlive() {
        List<String> channels = new ArrayList<>();
        String data = "{}";

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        reset(mTaskExecutor);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mSseConnectionManager.onKeepAlive();

        ArgumentCaptor<Long> downNotificationTime = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).schedule(any(SseConnectionManagerImpl.SseKeepAliveTimer.class), downNotificationTime.capture(), any());
        Assert.assertEquals(70L, downNotificationTime.getValue().longValue());
    }

    @Test
    public void setupSseTokenExpirationTimerOnAuth() throws InterruptedException {
        List<String> channels = dummyChannels();
        String data = "{}";

        long expirationTime = System.currentTimeMillis() / 1000 + 603;

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.start();
        reset(mTaskExecutor);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true, expirationTime
                )));

        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<Long> expirationCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mTaskExecutor, times(1)).schedule(any(SseConnectionManagerImpl.SseTokenExpiredTimer.class), expirationCaptor.capture(), any());
        Assert.assertEquals(3, expirationCaptor.getValue().longValue());
    }

    @Test
    public void refreshSseToken() throws InterruptedException {
        String keepAliveTaskId = "keepAliveTaskId";
        List<String> channels = dummyChannels();
        String data = "{}";
        mSseConnectionManager.start();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        when(mTaskExecutor.schedule(
                any(SseConnectionManagerImpl.SseKeepAliveTimer.class), anyLong(), any()))
                .thenReturn(keepAliveTaskId);


        SseConnectionManagerImpl.SseTokenExpiredTimer tokenExpiredTimerTask
                = mSseConnectionManager.new SseTokenExpiredTimer();

        mSseConnectionManager.onKeepAlive();
        tokenExpiredTimerTask.execute();
        verify(mTaskExecutor, times(1)).stopTask(keepAliveTaskId);
        verify(mSseClient, times(1)).disconnect();
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void scheduleDisconnectOnBg() throws InterruptedException {
        // Connection manager has to schedule disconnection in x secs
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        verify(mSseClient, times(1)).scheduleDisconnection(anyLong());
    }

    @Test
    public void scheduledDisconnectFiredOnBg() throws InterruptedException {
        // Disconnection is fired on bg after x secs.
        // All timers should be cancelled
        // On App FG authentication should be triggered

        // Return id when scheduling task to make it possible to
        // stop tasks
        when(mTaskExecutor.schedule(
                any(), anyLong(), any()))
                .thenReturn("id2");
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        mSseConnectionManager.onDisconnect();

        verify(mSseClient, times(1)).scheduleDisconnection(anyLong());
        // TODO: Figure out how to make this work
        //verify(mTaskExecutor, times(2)).stopTask(anyString());
    }

    @Test
    public void cancelTimerOnWhenNoDisconnectOnFg() throws InterruptedException {
        // When comming to foreground disconnection timer should be
        // cancelled if onDisconnect method from SSE client wasn't called
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(true);
        when(mSseClient.readyState()).thenReturn(SseClient.CLOSED);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
    }

    @Test
    public void noConnectOnFgWhileStillConnected() throws InterruptedException {
        // If SSE connection is onpenned when app FG, avoid triggering auth
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(true);
        when(mSseClient.readyState()).thenReturn(SseClient.OPEN);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
        verify(mTaskExecutor, never()).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void connectOnFgWhileOpenAndCancelFail() throws InterruptedException {
        // Tests that connection is triggered if ready state is open
        // but couldn't cancel scheduled disconnection because it was triggered
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(false);
        when(mSseClient.readyState()).thenReturn(SseClient.OPEN);
        mSseConnectionManager.start();
        // Should be here to avoid counting start two
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.pause();
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void noTriggerAuthOnFgIfAlreadyTriggered() throws InterruptedException {
        // If SSE auth was triggered when app FG, avoid triggering again
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(true);
        when(mSseClient.readyState()).thenReturn(SseClient.CLOSED);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
        verify(mTaskExecutor, never()).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void triggerAuthAgainIfSuccessResultOnBg() throws InterruptedException {
        // This tests simulates app losting connection previous to go BG
        // so SSE auth is triggered previous to go BG. It should be cancelled if
        // getting success result while in BG (taskExecuted call)
        // should stop auth/connection process
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(true);
        when(mSseClient.readyState()).thenReturn(SseClient.CLOSED);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK));
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void triggerAuthAgainIfErrorResultOnBg() throws InterruptedException {
        // This tests simulates app losting connection previous to go BG
        // so SSE auth is triggered previous to go BG. It should be cancelled if
        // getting error result while in BG (taskExecuted call)
        // should stop auth/connection process
        when(mSseClient.cancelDisconnectionTimer()).thenReturn(true);
        when(mSseClient.readyState()).thenReturn(SseClient.CLOSED);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mSseConnectionManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK));
        mSseConnectionManager.resume();
        verify(mSseClient, times(1)).cancelDisconnectionTimer();
        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(SseConnectionManagerImpl.class));
    }

    @Test
    public void sseClientError() throws InterruptedException {
        when(mSseClient.readyState()).thenReturn(SseClient.OPEN);
        mSseConnectionManager.start();
        mSseConnectionManager.onError(true);
        verify(mTaskExecutor, times(1)).schedule(any(SseConnectionManagerImpl.SseReconnectionTimer.class), anyLong(), isNull());
    }

    @Test
    public void sseClientErrorOnBg() throws InterruptedException {
        when(mSseClient.readyState()).thenReturn(SseClient.OPEN);
        mSseConnectionManager.start();
        mSseConnectionManager.pause();
        mSseConnectionManager.onError(true);
        verify(mTaskExecutor, never()).schedule(any(SseConnectionManagerImpl.SseReconnectionTimer.class), anyLong(), isNull());
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

}
