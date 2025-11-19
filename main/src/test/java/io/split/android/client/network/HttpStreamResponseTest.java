package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class HttpStreamResponseTest {

    private static final int HTTP_STATUS_OK = 200;
    private static final int HTTP_STATUS_BAD_REQUEST = 400;

    @Mock
    private BufferedReader mockBufferedReader;
    
    @Mock
    private Socket mockTunnelSocket;
    
    @Mock
    private Socket mockOriginSocket;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createFromTunnelSocketReturnsValidResponse() {
        // Create response with both sockets
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_OK,
                mockBufferedReader,
                mockTunnelSocket,
                mockOriginSocket
        );

        // Verify the response is created correctly
        assertNotNull(response);
        assertEquals(HTTP_STATUS_OK, response.getHttpStatus());
    }

    @Test
    public void createFromTunnelSocketWithNullSocketsReturnsValidResponse() {
        // Create response with null sockets
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_BAD_REQUEST,
                mockBufferedReader,
                null,
                null
        );

        // Verify the response is created correctly
        assertNotNull(response);
        assertEquals(HTTP_STATUS_BAD_REQUEST, response.getHttpStatus());
    }

    @Test
    public void closeSuccessfullyClosesAllResources() throws IOException {
        // Create response with both sockets
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_OK,
                mockBufferedReader,
                mockTunnelSocket,
                mockOriginSocket
        );

        // Close the response
        response.close();

        // Verify all resources were closed in the correct order
        verify(mockBufferedReader, times(1)).close();
        verify(mockOriginSocket, times(1)).close();
        verify(mockTunnelSocket, times(1)).close();
    }

    @Test
    public void closeWithNullSocketsOnlyClosesBufferedReader() throws IOException {
        // Create response with null sockets
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_OK,
                mockBufferedReader,
                null,
                null
        );

        // Close the response
        response.close();

        // Verify only the BufferedReader was closed
        verify(mockBufferedReader, times(1)).close();
        verifyNoMoreInteractions(mockTunnelSocket, mockOriginSocket);
    }

    @Test
    public void closeWithSameTunnelAndOriginSocketClosesSocketOnce() throws IOException {
        // Create response with the same socket for tunnel and origin
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_OK,
                mockBufferedReader,
                mockTunnelSocket,
                mockTunnelSocket
        );

        // Close the response
        response.close();

        // Verify BufferedReader was closed
        verify(mockBufferedReader, times(1)).close();
        
        // Verify tunnel socket was closed only once (since it's the same as origin socket)
        verify(mockTunnelSocket, times(1)).close();
    }

    @Test
    public void closeWithExceptionsSucceeds() throws IOException {
        // Setup mocks to throw exceptions when closed
        doThrow(new IOException("BufferedReader close error")).when(mockBufferedReader).close();
        doThrow(new IOException("Origin socket close error")).when(mockOriginSocket).close();
        doThrow(new IOException("Tunnel socket close error")).when(mockTunnelSocket).close();

        // Create response with both sockets
        HttpStreamResponseImpl response = HttpStreamResponseImpl.createFromTunnelSocket(
                HTTP_STATUS_OK,
                mockBufferedReader,
                mockTunnelSocket,
                mockOriginSocket
        );

        // Close the response - should not throw exceptions
        response.close();

        // Verify all resources were attempted to be closed despite exceptions
        verify(mockBufferedReader, times(1)).close();
        verify(mockOriginSocket, times(1)).close();
        verify(mockTunnelSocket, times(1)).close();
    }
}
