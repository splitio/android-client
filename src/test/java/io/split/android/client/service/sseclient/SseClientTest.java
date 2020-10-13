package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.sseclient.sseclient.SseClientImpl;
import io.split.android.client.service.sseclient.sseclient.SseHandler;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SseClientTest {

    @Mock
    HttpClient mHttpClient;

    @Mock
    EventStreamParser mParser;

    @Mock
    SseHandler mSseHandler;

    @Mock
    SseJwtToken mJwt;

    BlockingQueue mData;

    SseClient mClient;

    URI mUri;

    @Before
    public void setup() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        mUri = new URI("http://api/sse");
        mClient = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        mData = new LinkedBlockingDeque();
    }

    @Test
    public void onConnect() throws InterruptedException, HttpException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);
        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
    }

    @Test
    public void onMessage() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);

        // Simulate message arrived
        when(mParser.parseLineAndAppendValue(any(), any())).thenReturn(true).thenReturn(false);
        when(mParser.isKeepAlive(any())).thenReturn(false);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
        verify(mSseHandler, times(1)).handleIncomingMessage(any());
    }

    @Test
    public void onKeepAlive() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);

        // Simulate message arrived
        when(mParser.parseLineAndAppendValue(any(), any())).thenReturn(true).thenReturn(false);
        when(mParser.isKeepAlive(any())).thenReturn(true);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void clientError() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(false);
        when(response.isClientRelatedError()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(false);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void ioException() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        BufferedReader reader = Mockito.mock(BufferedReader.class);
        when(reader.readLine()).thenThrow(IOException.class);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(reader);
        when(request.execute()).thenReturn(response);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(true);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void noClientError() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(false);
        when(response.isClientRelatedError()).thenReturn(false);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        client.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(true);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void disconnect() throws InterruptedException, HttpException, IOException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        TestConnListener connListener = spy(new TestConnListener(onOpenLatch));
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);

        HttpStreamResponse response = new HttpStreamResponseMock(200, mData);

        when(request.execute()).thenReturn(response);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClientImpl(mUri, mHttpClient, mParser, mSseHandler);
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.connect(mJwt, new TestConnListener(onOpenLatch) {
                    @Override
                    public void onConnectionSuccess() {
                        super.onConnectionSuccess();
                    }
                });
            }
        }).start();

        Thread.sleep(500);
        client.disconnect();
        verify(mSseHandler, never()).handleError(anyBoolean());
    }

    private void setupJwt(List<String> channels, long issuedAt, long expirationTime, String rawToken) {
        when(mJwt.getChannels()).thenReturn(channels);
        when(mJwt.getIssuedAtTime()).thenReturn(issuedAt);
        when(mJwt.getExpirationTime()).thenReturn(expirationTime);
        when(mJwt.getRawJwt()).thenReturn(rawToken);
    }

//    @Test
//    public void cancelScheduledDisconnectTimer() throws InterruptedException {
//        mClient = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
//        mClient.scheduleDisconnection(50);
//        sleep(1000);
//        boolean result = mClient.cancelDisconnectionTimer();
//        Assert.assertTrue(result);
//    }
//
//    @Test
//    public void failedCancelScheduledDisconnectTimer() throws InterruptedException {
//        SseClient client = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
//        client.scheduleDisconnection(DUMMY_DELAY);
//        sleep(DUMMY_DELAY + 2000);
//        boolean result = client.cancelDisconnectionTimer();
//        Assert.assertFalse(result);
//    }
//
//    @Test
//    public void disconnectTriggered() throws InterruptedException, HttpException, IOException {
//        Listener listener = new Listener();
//
//        CountDownLatch onDisconnectLatch = new CountDownLatch(1);
//        listener.mOnDisconnectLatch = onDisconnectLatch;
//        listener = spy(listener);
//
//        List<String> dummyChannels = new ArrayList<String>();
//        dummyChannels.add("dummychanel");
//        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);
//        HttpStreamResponse response = new HttpStreamResponseMock(200, mData);
//
//        when(request.execute()).thenReturn(response);
//        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
//        SseClient client = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
//        client.setListener(listener);
//        client.connect("pepetoken", dummyChannels);
//
//        client = spy(client);
//        client.scheduleDisconnection(DUMMY_DELAY);
//        onDisconnectLatch.await(10, TimeUnit.SECONDS);
//        long readyState = client.readyState();
//
//        verify(client, times(1)).disconnect();
//        verify(listener, never()).onError(anyBoolean());
//        verify(listener, times(1)).onDisconnect();
//        Assert.assertEquals(SseClient.CLOSED, readyState);
//    }


    private BufferedReader dummyData() {
        InputStream inputStream = new ByteArrayInputStream("dummydata\n".getBytes(Charset.forName("UTF-8")));
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    private static class TestConnListener implements SseClientImpl.ConnectionListener {
        CountDownLatch mConnLatch;
        public TestConnListener(CountDownLatch connLatch) {
            mConnLatch = connLatch;
        }

        @Override
        public void onConnectionSuccess() {
            mConnLatch.countDown();
        }
    }


}

