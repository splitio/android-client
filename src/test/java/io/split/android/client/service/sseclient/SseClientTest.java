package io.split.android.client.service.sseclient;

import org.junit.Assert;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SseClientTest {

    @Mock
    HttpClient mHttpClient;

    @Mock
    ScheduledThreadPoolExecutor mExecutor;

    @Mock
    EventStreamParser mParser;

    @Mock
    SseClientListener mListener;

    BlockingQueue mData;

    SseClient mClient;

    URI mUri;

    private static long DUMMY_DELAY = 1;
    private static int POOL_SIZE = 2;

    @Before
    public void setup() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        mUri = new URI("http://api/sse");
        mClient = new SseClient(mUri, mHttpClient, mParser, mExecutor);
        mData = new LinkedBlockingDeque();
    }

    @Test
    public void onConnect() throws InterruptedException, HttpException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);
        Listener listener = new Listener(onOpenLatch);
        listener = spy(listener);
        List<String> dummyChannels = new ArrayList<String>();
        dummyChannels.add("dummychanel");
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);
        HttpStreamResponse response = Mockito.mock(HttpStreamResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getBufferedReader()).thenReturn(dummyData());
        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
        client.setListener(mListener);
        client.connect("pepetoken", dummyChannels);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        long readyState = client.readyState();

        verify(mListener, times(1)).onOpen();
    }

    @Test
    public void scheduleDisconnect() {
        when(mExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(Mockito.mock(ScheduledFuture.class));
        mClient.scheduleDisconnection(DUMMY_DELAY);
        verify(mExecutor, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void cancelScheduledDisconnectTimer() throws InterruptedException {
        mClient = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
        mClient.scheduleDisconnection(50);
        sleep(1000);
        boolean result = mClient.cancelDisconnectionTimer();
        Assert.assertTrue(result);
    }

    @Test
    public void failedCancelScheduledDisconnectTimer() throws InterruptedException {
        SseClient client = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
        client.scheduleDisconnection(DUMMY_DELAY);
        sleep(DUMMY_DELAY + 2000);
        boolean result = client.cancelDisconnectionTimer();
        Assert.assertFalse(result);
    }

    @Test
    public void disconnectTriggered() throws InterruptedException, HttpException, IOException {
        Listener listener = new Listener();

        CountDownLatch onDisconnectLatch = new CountDownLatch(1);
        listener.mOnDisconnectLatch = onDisconnectLatch;
        listener = spy(listener);

        List<String> dummyChannels = new ArrayList<String>();
        dummyChannels.add("dummychanel");
        HttpStreamRequest request = Mockito.mock(HttpStreamRequest.class);
        HttpStreamResponse response = new HttpStreamResponseMock(200, mData);

        when(request.execute()).thenReturn(response);
        when(mHttpClient.streamRequest(any(URI.class))).thenReturn(request);
        SseClient client = new SseClient(mUri, mHttpClient, mParser, new ScheduledThreadPoolExecutor(POOL_SIZE));
        client.setListener(listener);
        client.connect("pepetoken", dummyChannels);

        client = spy(client);
        client.scheduleDisconnection(DUMMY_DELAY);
        onDisconnectLatch.await(10, TimeUnit.SECONDS);
        long readyState = client.readyState();

        verify(client, times(1)).disconnect();
        verify(listener, never()).onError(anyBoolean());
        verify(listener, times(1)).onDisconnect();
        Assert.assertEquals(SseClient.CLOSED, readyState);
    }

    private class Listener implements SseClientListener {
        CountDownLatch mOnOpenLatch;
        CountDownLatch mOnErrorLatch;
        CountDownLatch mOnDisconnectLatch;

        boolean onDisconnectCalled = false;

        public Listener() {
        }

        public Listener(CountDownLatch mOnOpenLatch) {
            mOnOpenLatch = mOnOpenLatch;
        }

        @Override
        public void onOpen() {
            System.out.println("SseClientTest: OnOPEN!!!!");
            if (mOnOpenLatch != null) {
                mOnOpenLatch.countDown();
            }
        }

        @Override
        public void onMessage(Map<String, String> values) {
            System.out.println("SseClientTest: OnMsg!!!!");
        }

        @Override
        public void onError(boolean isRecoverable) {
            System.out.println("SseClientTest: OnError!!!!");
            if (mOnErrorLatch != null) {
                mOnErrorLatch.countDown();
            }
        }

        @Override
        public void onKeepAlive() {

        }

        @Override
        public void onDisconnect() {
            onDisconnectCalled = true;
            if (mOnDisconnectLatch != null) {
                mOnDisconnectLatch.countDown();
            }
        }
    }

    private BufferedReader dummyData() {
        InputStream inputStream = new ByteArrayInputStream("hola".getBytes(Charset.forName("UTF-8")));

        return new BufferedReader(new InputStreamReader(inputStream));
    }


}

