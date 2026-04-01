package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.service.sseclient.sseclient.DefaultSseClient;
import io.split.android.client.service.sseclient.sseclient.EventSourceClient;
import io.split.android.client.service.sseclient.sseclient.SseClient;
import io.split.android.client.service.sseclient.sseclient.SseHandler;

public class SseClientTest {

    @Mock
    EventSourceClient mEventSourceClient;

    @Mock
    SseHandler mSseHandler;

    @Mock
    SseJwtToken mJwt;

    SseClient mClient;

    URI mUri;

    @Before
    public void setup() throws URISyntaxException {
        MockitoAnnotations.openMocks(this);
        mUri = new URI("http://api/sse");
        mClient = new DefaultSseClient(mUri, mEventSourceClient, mSseHandler);
    }

    @Test
    public void onConnect() throws InterruptedException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        SseClient.ConnectionListener connListener = spy(new TestConnListener(onOpenLatch));
        when(mSseHandler.isConnectionConfirmed(any())).thenReturn(true);

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onOpen();
            Map<String, String> event = new HashMap<>();
            event.put("data", "somedata");
            handler.onMessage(event);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
    }

    @Test
    public void onConnectNotConfirmed() throws InterruptedException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        SseClient.ConnectionListener connListener = spy(new TestConnListener(onOpenLatch));
        when(mSseHandler.isConnectionConfirmed(any())).thenReturn(false);
        when(mSseHandler.isRetryableError(any())).thenReturn(true);

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onOpen();
            Map<String, String> event = new HashMap<>();
            event.put("data", "error");
            handler.onMessage(event);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, never()).onConnectionSuccess();
        verify(mSseHandler, times(1)).handleError(true);
        verify(mEventSourceClient, times(1)).disconnect();
    }

    @Test
    public void onMessage() throws InterruptedException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        SseClient.ConnectionListener connListener = spy(new TestConnListener(onOpenLatch));
        when(mSseHandler.isConnectionConfirmed(any())).thenReturn(true);

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onOpen();
            // First message confirms connection
            Map<String, String> event1 = new HashMap<>();
            event1.put("data", "first");
            handler.onMessage(event1);
            // Second message is a real notification
            Map<String, String> event2 = new HashMap<>();
            event2.put("data", "second");
            handler.onMessage(event2);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
        // Both messages are routed to handleIncomingMessage
        verify(mSseHandler, times(2)).handleIncomingMessage(any());
    }

    @Test
    public void onKeepAlive() throws InterruptedException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);

        SseClient.ConnectionListener connListener = spy(new TestConnListener(onOpenLatch));
        when(mSseHandler.isConnectionConfirmed(any())).thenReturn(true);

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onOpen();
            // Keepalive event confirms connection but is not routed to handler
            Map<String, String> keepalive = new HashMap<>();
            keepalive.put("event", "keepalive");
            handler.onMessage(keepalive);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        onOpenLatch.await(1000, TimeUnit.MILLISECONDS);

        verify(connListener, times(1)).onConnectionSuccess();
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void clientError() {
        SseClient.ConnectionListener connListener = spy(new TestConnListener(new CountDownLatch(1)));

        // EventSourceClient reports non-retryable error
        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onError(false);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(false);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void ioException() {
        SseClient.ConnectionListener connListener = spy(new TestConnListener(new CountDownLatch(1)));

        // EventSourceClient reports retryable error (like IOException)
        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onError(true);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(true);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void disconnect() throws InterruptedException {
        CountDownLatch onOpenLatch = new CountDownLatch(1);
        SseClient.ConnectionListener connListener = spy(new TestConnListener(onOpenLatch));

        // EventSourceClient simulates long-lived connection
        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onOpen();
            // Simulate blocking connection
            Thread.sleep(2000);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        new Thread(() -> mClient.connect(mJwt, connListener)).start();

        Thread.sleep(500);
        mClient.disconnect();

        verify(mEventSourceClient, times(1)).disconnect();
    }

    @Test
    public void nonRetryableErrorOnConnection() {
        SseClient.ConnectionListener connListener = spy(new TestConnListener(new CountDownLatch(1)));

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onError(false);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(false);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    @Test
    public void retryableErrorOnConnection() {
        SseClient.ConnectionListener connListener = spy(new TestConnListener(new CountDownLatch(1)));

        doAnswer(invocation -> {
            EventSourceClient.EventHandler handler = invocation.getArgument(1);
            handler.onError(true);
            return null;
        }).when(mEventSourceClient).connect(any(URI.class), any(EventSourceClient.EventHandler.class));

        mClient.connect(mJwt, connListener);

        verify(mSseHandler, times(1)).handleError(true);
        verify(mSseHandler, never()).handleIncomingMessage(any());
    }

    private static class TestConnListener implements SseClient.ConnectionListener {
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
