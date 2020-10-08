package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.sseclient.NewPushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.NewSseClient;
import io.split.android.client.service.sseclient.sseclient.NewSseClientImpl;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticationResult;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.sseclient.sseclient.SseDisconnectionTimer;
import io.split.android.client.service.sseclient.sseclient.SseRefreshTokenTimer;
import io.split.android.fake.NewSseClientMock;

import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewPushNotificationManagerTest {

    private static final String DUMMY_TOKEN = "DUMMY_TOKEN";
    private static final int POOL_SIZE = 1;

    @Mock
    ScheduledThreadPoolExecutor mExecutor;

    @Mock
    SseAuthenticator mAuthenticator;

    @Mock
    PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    SseRefreshTokenTimer mRefreshTokenTimer;

    @Mock
    SseDisconnectionTimer mDisconnectionTimer;

    @Mock
    SseJwtToken mJwt;


    NewPushNotificationManager mPushManager;

    URI mUri;

    @Before
    public void setup() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        mUri = new URI("http://api/sse");
    }

    @Test
    public void connectOk() throws InterruptedException, HttpException {
        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        setupOkAuthResponse();

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_SUBSYSTEM_UP);

        ArgumentCaptor<Long> issuedAt = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> expirationTime = ArgumentCaptor.forClass(Long.class);
        verify(mRefreshTokenTimer, times(1)).schedule(issuedAt.capture(), expirationTime.capture());
        Assert.assertEquals(1000L, issuedAt.getValue().longValue());
        Assert.assertEquals(10000L, expirationTime.getValue().longValue());
    }

    @Test
    public void connectClientError() throws InterruptedException, HttpException {
        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(false, false, false, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_NON_RETRYABLE_ERROR);
    }

    @Test
    public void connectStreamingDisabled() throws InterruptedException, HttpException {
        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(true, false, false, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(PushStatusEvent.EventType.PUSH_SUBSYSTEM_DOWN, messageCaptor.getValue().getMessage());
    }

    @Test
    public void connectOtherError() throws InterruptedException, HttpException {
        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(false, true, false, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_RETRYABLE_ERROR);
    }

    @Test
    public void pause() throws InterruptedException, HttpException {
        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));
        setupOkAuthResponse();
        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        mPushManager.pause();
        verify(mDisconnectionTimer, times(1)).schedule(any());
        verify(mDisconnectionTimer, never()).cancel();
    }

    @Test
    public void resume() throws InterruptedException, HttpException {

        NewSseClientMock sseClient = new NewSseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new NewPushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, new ScheduledThreadPoolExecutor(POOL_SIZE));
        setupOkAuthResponse();
        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        mPushManager.pause();
        mPushManager.resume();
        verify(mDisconnectionTimer, times(1)).schedule(any());
        verify(mDisconnectionTimer, times(1)).cancel();
    }

    private void setupOkAuthResponse() {
        when(mJwt.getChannels()).thenReturn(Arrays.asList("dummy"));
        when(mJwt.getIssuedAtTime()).thenReturn(1000L);
        when(mJwt.getExpirationTime()).thenReturn(10000L);

        when(mJwt.getRawJwt()).thenReturn(DUMMY_TOKEN);
        SseAuthenticationResult result = new SseAuthenticationResult(true, true, true, mJwt);

        when(mAuthenticator.authenticate()).thenReturn(result);
    }

    private BufferedReader dummyData() {
        InputStream inputStream = new ByteArrayInputStream("hola".getBytes(Charset.forName("UTF-8")));

        return new BufferedReader(new InputStreamReader(inputStream));
    }


}

