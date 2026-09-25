package io.split.android.client.service.sseclient.sseclient;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.net.URI;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.service.sseclient.spi.StreamingTransport;

public class HttpClientStreamingTransportTest {

    /**
     * Reproduces a crash seen when a proxy/tunnel setup failure surfaces as a plain IOException,
     * which HttpStreamRequestImpl wraps into an HttpException with no status code (null).
     * Propagating that null status code used to blow up on unboxing instead of surfacing as a
     * StreamingTransportException.
     */
    @Test
    public void executeWrapsHttpExceptionWithNullStatusCodeWithoutThrowingNpe() throws Exception {
        HttpStreamRequest request = mock(HttpStreamRequest.class);
        when(request.execute()).thenThrow(new HttpException("Something happened while retrieving data: connection reset"));

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.streamRequest(org.mockito.ArgumentMatchers.any(URI.class))).thenReturn(request);

        StreamingTransport transport = new HttpClientStreamingTransport(httpClient);
        StreamingTransport.StreamingConnection connection = transport.connect(URI.create("https://example.com/sse"));

        try {
            connection.execute();
        } catch (StreamingTransport.StreamingTransportException e) {
            assertNull(e.getStatusCode());
            return;
        }

        throw new AssertionError("Expected StreamingTransportException to be thrown");
    }
}
