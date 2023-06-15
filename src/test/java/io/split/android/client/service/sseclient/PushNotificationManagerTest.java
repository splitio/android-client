package io.split.android.client.service.sseclient;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.network.HttpException;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticationResult;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.sseclient.sseclient.SseDisconnectionTimer;
import io.split.android.client.service.sseclient.sseclient.SseRefreshTokenTimer;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.fake.SseClientMock;

public class PushNotificationManagerTest {

    private static final String DUMMY_TOKEN = "DUMMY_TOKEN";
    private static final int POOL_SIZE = 1;

    @Mock
    private ScheduledThreadPoolExecutor mExecutor;

    @Mock
    private SseAuthenticator mAuthenticator;

    @Mock
    private PushManagerEventBroadcaster mBroadcasterChannel;

    @Mock
    private SseRefreshTokenTimer mRefreshTokenTimer;

    @Mock
    private SseDisconnectionTimer mDisconnectionTimer;

    @Mock
    private SseJwtToken mJwt;

    @Mock
    private SseAuthenticationResult mResult;

    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    PushNotificationManager mPushManager;

    URI mUri;

    @Before
    public void setup() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        mUri = new URI("http://api/sse");
    }

    @Test
    public void connectOk() throws InterruptedException {
        setupOkAuthResponse();
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        long time = System.currentTimeMillis();
        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        time = System.currentTimeMillis() - time;
        verify(mBroadcasterChannel).pushMessage(argThat(argument -> argument.getMessage().equals(PushStatusEvent.EventType.PUSH_SUBSYSTEM_UP)));
        verify(mBroadcasterChannel).pushMessage(argThat(argument -> argument.getMessage().equals(PushStatusEvent.EventType.PUSH_DELAY_RECEIVED)));

        ArgumentCaptor<Long> issuedAt = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> expirationTime = ArgumentCaptor.forClass(Long.class);
        verify(mRefreshTokenTimer, times(1)).schedule(issuedAt.capture(), expirationTime.capture());
        Assert.assertEquals(1000L, issuedAt.getValue().longValue());
        Assert.assertEquals(10000L, expirationTime.getValue().longValue());
        assertTrue(time < 2000);
    }

    @Test
    public void connectOkWithDelay() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        setupOkAuthResponse(4);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        long time = System.currentTimeMillis();
        mPushManager.start();
        sseClient.mConnectLatch.await(10, TimeUnit.SECONDS);
        time = System.currentTimeMillis() - time;

        verify(mBroadcasterChannel, times(1)).pushMessage(argThat(argument -> argument.getMessage().equals(PushStatusEvent.EventType.PUSH_SUBSYSTEM_UP)));
        verify(mBroadcasterChannel).pushMessage(argThat(argument -> argument.getMessage().equals(PushStatusEvent.EventType.PUSH_DELAY_RECEIVED)));
        ArgumentCaptor<Long> issuedAt = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> expirationTime = ArgumentCaptor.forClass(Long.class);
        verify(mRefreshTokenTimer).schedule(issuedAt.capture(), expirationTime.capture());
        Assert.assertEquals(1000L, issuedAt.getValue().longValue());
        Assert.assertEquals(10000L, expirationTime.getValue().longValue());
        assertTrue(time > 3000);
    }

    @Test
    public void connectClientError() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(false, false, false, 0, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_NON_RETRYABLE_ERROR);
    }

    @Test
    public void connectStreamingDisabled() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(true, false, false, 0, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(PushStatusEvent.EventType.PUSH_SUBSYSTEM_DOWN, messageCaptor.getValue().getMessage());
    }

    @Test
    public void connectOtherError() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(false, true, false, 0, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<PushStatusEvent> messageCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcasterChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(messageCaptor.getValue().getMessage(), PushStatusEvent.EventType.PUSH_RETRYABLE_ERROR);
    }

    @Test
    public void pause() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));
        setupOkAuthResponse();
        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        mPushManager.pause();
        verify(mDisconnectionTimer, times(1)).schedule(any());
        verify(mDisconnectionTimer, never()).cancel();
    }

    @Test
    public void resume() throws InterruptedException, HttpException {

        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));
        setupOkAuthResponse();
        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        mPushManager.pause();
        mPushManager.resume();
        verify(mDisconnectionTimer, times(1)).schedule(any());
        verify(mDisconnectionTimer, times(1)).cancel();
    }

    @Test
    public void successfulConnectionTracksTokenRefreshInTelemetry() throws InterruptedException {
        performSuccessfulConnection();

        verify(mTelemetryRuntimeProducer).recordTokenRefreshes();
        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.TOKEN), longThat(argument -> argument > 0));
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(any(TokenRefreshStreamingEvent.class));
    }

    @Test
    public void connectErrorTracksAuthRejectionInTelemetry() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(false, false, false, 0, null);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        verify(mTelemetryRuntimeProducer).recordAuthRejections();
    }

    @Test
    public void connectErrorTracksSyncErrorInTelemetryWhenThereIsHttpStatus() throws InterruptedException, HttpException {
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        SseAuthenticationResult result = new SseAuthenticationResult(
                false, false, false, 0, null, 500);

        when(mAuthenticator.authenticate()).thenReturn(result);

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.TOKEN, 500);
    }

    @Test
    public void authenticationLatencyIsTracked() throws InterruptedException {
        performSuccessfulConnection();
        Thread.sleep(1000);

        verify(mTelemetryRuntimeProducer).recordSyncLatency(eq(OperationType.TOKEN), anyLong());
    }

    @Test
    public void stopDisconnectsClient() {
        SseClient sseClient = mock(SseClient.class);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        mPushManager.stop();

        verify(mDisconnectionTimer).cancel();
        verify(mRefreshTokenTimer).cancel();
        verify(sseClient).disconnect();
    }

    private void performSuccessfulConnection() throws InterruptedException {
        setupOkAuthResponse();
        SseClientMock sseClient = new SseClientMock();
        sseClient.mConnectLatch = new CountDownLatch(1);
        mPushManager = new PushNotificationManager(mBroadcasterChannel, mAuthenticator, sseClient, mRefreshTokenTimer,
                mDisconnectionTimer, mTelemetryRuntimeProducer, new ScheduledThreadPoolExecutor(POOL_SIZE));

        mPushManager.start();
        sseClient.mConnectLatch.await(2, TimeUnit.SECONDS);
    }

    private void setupOkAuthResponse() {
        setupOkAuthResponse(0L);
    }

    private void setupOkAuthResponse(long delay) {
        when(mJwt.getChannels()).thenReturn(Arrays.asList("dummy"));
        when(mJwt.getIssuedAtTime()).thenReturn(1000L);
        when(mJwt.getExpirationTime()).thenReturn(10000L);

        when(mJwt.getRawJwt()).thenReturn(DUMMY_TOKEN);

        when(mResult.isSuccess()).thenReturn(true);
        when(mResult.isErrorRecoverable()).thenReturn(true);
        when(mResult.isPushEnabled()).thenReturn(true);
        when(mResult.getJwtToken()).thenReturn(mJwt);
        when(mResult.getSseConnectionDelay()).thenReturn(delay);

        when(mAuthenticator.authenticate()).thenReturn(mResult);
    }

    private BufferedReader dummyData() {
        InputStream inputStream = new ByteArrayInputStream("hola" .getBytes(Charset.forName("UTF-8")));

        return new BufferedReader(new InputStreamReader(inputStream));
    }
}
