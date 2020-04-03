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

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseauthentication.SseAuthenticationTask;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushNotificationManagerTest {

    private static final String TOKEN = "THETOKEN";

    @Mock
    SseClient mSseClient;
    @Mock
    SplitTaskExecutor mTaskExecutor;
    @Mock
    SplitTaskFactory mSplitTaskFactory;
    @Mock
    SyncManagerFeedbackChannel mFeedbackChannel;

    @Mock
    SseAuthenticationTask mSseAuthTask;

    @Mock
    NotificationProcessor mNotificationProcessor;


    PushNotificationManager mPushManager;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPushManager = new PushNotificationManager(mSseClient, mTaskExecutor,
                mSplitTaskFactory, mNotificationProcessor, mFeedbackChannel);
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
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor = ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.PUSH_ENABLED, messageCaptor.getValue().getMessage());
    }

    @Test
    public void sseAuthError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK));

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(any(), any());
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor = ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.PUSH_DISABLED, messageCaptor.getValue().getMessage());
    }

    @Test
    public void authOkAndSubscritionToSseError() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));
        mPushManager.onError();

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mSseClient, times(1)).connect(TOKEN, channels);
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor = ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.PUSH_DISABLED, messageCaptor.getValue().getMessage());
    }

    @Test
    public void authOkAndChannelsPushDisabled() {
        List<String> channels = dummyChannels();

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, false)));

        verify(mTaskExecutor, times(1)).submit(any(SseAuthenticationTask.class), any(PushNotificationManager.class));
        verify(mSseClient, never()).connect(TOKEN, channels);
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor = ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.PUSH_DISABLED, messageCaptor.getValue().getMessage());
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
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor = ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.PUSH_DISABLED, messageCaptor.getValue().getMessage());
    }

    @Test
    public void onMessage() {
        List<String> channels = new ArrayList<>();
        String data = "{}";

        when(mSplitTaskFactory.createSseAuthenticationTask()).thenReturn(mSseAuthTask);
        mPushManager.start();
        reset(mTaskExecutor);
        mPushManager.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                buildAuthMap(TOKEN, channels, true, true)));

        mPushManager.onMessage(message(data));

        verify(mNotificationProcessor, times(1)).process(data);
        verify(mTaskExecutor, times(1)).schedule(any(SplitTask.class), anyLong(), any());
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

        verify(mTaskExecutor, times(1)).schedule(any(SplitTask.class), anyLong(), any());
    }

    @After
    public void teardDown() {
        reset();
    }

    private Map<String, Object> buildAuthMap(String token, List<String> channels,
                                             boolean isApiKeyValid, boolean isStreamingEnabled) {
        Map<String, Object> data = new HashMap<>();
        data.put(SplitTaskExecutionInfo.SSE_TOKEN, token);
        data.put(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM, channels);
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
