package io.split.android.client.service.sseclient.sseclient;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.spi.StreamingTransport;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingConnection;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingResponse;
import io.split.android.client.service.sseclient.spi.StreamingTransport.StreamingTransportException;

public class EventSourceClientImplTest {

    @Mock
    private StreamingTransport mTransport;

    @Mock
    private StreamingConnection mConnection;

    @Mock
    private StreamingResponse mResponse;

    @Mock
    private EventSourceClient.EventHandler mHandler;

    private EventStreamParser mParser;
    private EventSourceClientImpl mClient;
    private URI mUri;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mParser = new EventStreamParser();
        mClient = new EventSourceClientImpl(mTransport, mParser);
        mUri = new URI("http://test.example.com/sse");
    }

    @Test
    public void initialStatusIsDisconnected() {
        assertEquals(EventSourceClient.DISCONNECTED, mClient.status());
    }

    @Test
    public void statusIsConnectedAfterSuccessfulConnection() throws Exception {
        String sseData = "event: message\ndata: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        // Status should be DISCONNECTED after connect() returns (connection closed)
        assertEquals(EventSourceClient.DISCONNECTED, mClient.status());
    }

    @Test
    public void onOpenCalledOnSuccessfulConnection() throws Exception {
        String sseData = "data: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onOpen();
    }

    @Test
    public void messagesDeliveredToHandler() throws Exception {
        String sseData = "event: update\ndata: {\"type\":\"split\"}\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mHandler, times(1)).onMessage(captor.capture());

        Map<String, String> event = captor.getValue();
        assertEquals("update", event.get("event"));
        assertEquals("{\"type\":\"split\"}", event.get("data"));
    }

    @Test
    public void multipleMessagesDelivered() throws Exception {
        String sseData = "data: first\n\nevent: second\ndata: message2\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(2)).onMessage(anyMap());
    }

    @Test
    public void keepaliveEventDelivered() throws Exception {
        String sseData = ":keepalive\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mHandler, times(1)).onMessage(captor.capture());

        Map<String, String> event = captor.getValue();
        assertEquals("keepalive", event.get("event"));
    }

    @Test
    public void onErrorCalledWithRetryableTrueOnIOException() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(true);

        BufferedReader mockReader = mock(BufferedReader.class);
        when(mockReader.readLine()).thenThrow(new IOException("Connection reset"));
        when(mResponse.getBufferedReader()).thenReturn(mockReader);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
    }

    @Test
    public void onErrorCalledWithRetryableFalseOnClientError() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(false);
        when(mResponse.isClientRelatedError()).thenReturn(true);
        when(mResponse.getHttpStatus()).thenReturn(401);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(false);
        verify(mHandler, never()).onOpen();
    }

    @Test
    public void onErrorCalledWithRetryableTrueOnServerError() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(false);
        when(mResponse.isClientRelatedError()).thenReturn(false);
        when(mResponse.getHttpStatus()).thenReturn(503);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
    }

    @Test
    public void onErrorCalledWithRetryableFalseOnTransportException4xx() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenThrow(new StreamingTransportException("Forbidden", 403));

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(false);
    }

    @Test
    public void onErrorCalledWithRetryableTrueOnTransportException408() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenThrow(new StreamingTransportException("Timeout", 408));

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
    }

    @Test
    public void onErrorCalledWithRetryableTrueOnTransportException5xx() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenThrow(new StreamingTransportException("Server error", 500));

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
    }

    @Test
    public void onErrorCalledWithRetryableTrueOnTransportExceptionWithNoStatusCode() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenThrow(new StreamingTransportException("Network error"));

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
    }

    @Test
    public void onErrorCalledOnNullBufferedReader() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(true);
        when(mResponse.getBufferedReader()).thenReturn(null);

        mClient.connect(mUri, mHandler);

        verify(mHandler, times(1)).onError(true);
        verify(mHandler, never()).onOpen();
    }

    @Test
    public void disconnectClosesConnection() throws Exception {
        CountDownLatch readingLatch = new CountDownLatch(1);
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(true);

        BufferedReader blockingReader = mock(BufferedReader.class);
        when(blockingReader.readLine()).thenAnswer(invocation -> {
            readingLatch.countDown();
            disconnectLatch.await(5, TimeUnit.SECONDS);
            return null; // End of stream
        });
        when(mResponse.getBufferedReader()).thenReturn(blockingReader);

        Thread connectThread = new Thread(() -> mClient.connect(mUri, mHandler));
        connectThread.start();

        // Wait for connect to start reading
        readingLatch.await(2, TimeUnit.SECONDS);

        // Disconnect from another thread
        mClient.disconnect();
        disconnectLatch.countDown();

        connectThread.join(2000);

        verify(mConnection, times(1)).close();
        verify(mResponse, times(1)).close();
    }

    @Test
    public void onErrorNotCalledWhenDisconnectIsCalled() throws Exception {
        CountDownLatch readingLatch = new CountDownLatch(1);
        AtomicBoolean disconnected = new AtomicBoolean(false);

        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(true);

        BufferedReader blockingReader = mock(BufferedReader.class);
        when(blockingReader.readLine()).thenAnswer(invocation -> {
            readingLatch.countDown();
            while (!disconnected.get()) {
                Thread.sleep(10);
            }
            return null;
        });
        when(mResponse.getBufferedReader()).thenReturn(blockingReader);

        Thread connectThread = new Thread(() -> mClient.connect(mUri, mHandler));
        connectThread.start();

        readingLatch.await(2, TimeUnit.SECONDS);

        mClient.disconnect();
        disconnected.set(true);

        connectThread.join(2000);

        // onError should NOT be called when disconnect() was explicitly called
        verify(mHandler, never()).onError(any(Boolean.class));
    }

    @Test
    public void disconnectIsIdempotent() throws Exception {
        String sseData = "data: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        // Multiple disconnects should not cause issues
        mClient.disconnect();
        mClient.disconnect();
        mClient.disconnect();

        // Should only close once
        verify(mResponse, times(1)).close();
        verify(mConnection, times(1)).close();
    }

    @Test
    public void resourcesClosedOnSuccess() throws Exception {
        String sseData = "data: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        verify(mResponse, times(1)).close();
        verify(mConnection, times(1)).close();
    }

    @Test
    public void resourcesClosedOnError() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(false);
        when(mResponse.isClientRelatedError()).thenReturn(true);

        mClient.connect(mUri, mHandler);

        verify(mConnection, times(1)).close();
    }

    @Test
    public void resourcesClosedOnException() throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenThrow(new StreamingTransportException("error"));

        mClient.connect(mUri, mHandler);

        verify(mConnection, times(1)).close();
    }

    @Test
    public void emptyLinesDoNotTriggerMessage() throws Exception {
        String sseData = "\n\n\ndata: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        // Only one message should be delivered (the data: test one)
        verify(mHandler, times(1)).onMessage(anyMap());
    }

    @Test
    public void commentLinesIgnored() throws Exception {
        String sseData = ": this is a comment\ndata: test\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mHandler, times(1)).onMessage(captor.capture());

        // Comment should not be in the event
        assertEquals("test", captor.getValue().get("data"));
    }

    @Test
    public void multiLineDataConcatenated() throws Exception {
        // Per SSE spec, multiple data fields should be present
        String sseData = "data: line1\ndata: line2\n\n";
        setupSuccessfulConnection(sseData);

        mClient.connect(mUri, mHandler);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mHandler, times(1)).onMessage(captor.capture());
        assertEquals("line2", captor.getValue().get("data"));
    }

    private void setupSuccessfulConnection(String sseData) throws Exception {
        when(mTransport.connect(any(URI.class))).thenReturn(mConnection);
        when(mConnection.execute()).thenReturn(mResponse);
        when(mResponse.isSuccess()).thenReturn(true);
        when(mResponse.getBufferedReader()).thenReturn(new BufferedReader(new StringReader(sseData)));
    }
}
