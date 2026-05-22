package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;

public class HttpOverTunnelExecutorTest {

    private static final String CRLF = "\r\n";
    private HttpOverTunnelExecutor mExecutor;

    @Mock
    private Socket mSocket;

    private OutputStream mOutputStream;
    private InputStream mInputStream;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        mExecutor = new HttpOverTunnelExecutor();
        mOutputStream = new ByteArrayOutputStream();

        String httpResponse = "HTTP/1.1 200 OK" + CRLF + "Content-Length: 0\r\n\r\n";
        mInputStream = new ByteArrayInputStream(httpResponse.getBytes());

        when(mSocket.getOutputStream()).thenReturn(mOutputStream);
        when(mSocket.getInputStream()).thenReturn(mInputStream);
    }

    @Test
    public void postRequestWithBodyAndHeaders() throws IOException {
        URL url = new URL("https://test.com/path");
        String body = "{\"key\":\"value\"}";
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Custom-Header", "CustomValue");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.POST, headers, body, null);

        String expectedRequest = "POST /path HTTP/1.1" + CRLF
                + "Host: test.com" + CRLF
                + "Custom-Header: CustomValue" + CRLF
                + "Content-Length: 15" + CRLF
                + "Connection: close" + CRLF
                + CRLF
                + body;

        assertEquals(expectedRequest, mOutputStream.toString());
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
    }

    @Test
    public void getRequestWithQuery() throws IOException {
        URL url = new URL("http://test.com/path?q=1&v=2");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null);

        String expectedRequest = "GET /path?q=1&v=2 HTTP/1.1" + CRLF
                + "Host: test.com" + CRLF
                + "Connection: close" + CRLF
                + CRLF;

        assertEquals(expectedRequest, mOutputStream.toString());
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
    }

    @Test
    public void getRequestWithNonDefaultPort() throws IOException {
        URL url = new URL("http://test.com:8080/path");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null);

        String expectedRequest = "GET /path HTTP/1.1" + CRLF
                + "Host: test.com:8080" + CRLF
                + "Connection: close" + CRLF
                + CRLF;

        assertEquals(expectedRequest, mOutputStream.toString());
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
    }

    @Test
    public void getRequestWithEmptyPath() throws IOException {
        URL url = new URL("http://test.com");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null);

        String expectedRequest = "GET / HTTP/1.1" + CRLF
                + "Host: test.com" + CRLF
                + "Connection: close" + CRLF
                + CRLF;

        assertEquals(expectedRequest, mOutputStream.toString());
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
    }

    @Test(expected = IOException.class)
    public void requestThrowsIOException() throws IOException {
        URL url = new URL("http://test.com/path");
        when(mSocket.getOutputStream()).thenThrow(new IOException("Socket error"));

        mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null);
    }

    @Test
    public void getRequest() throws IOException {
        URL url = new URL("http://test.com/path");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null);

        String expectedRequest = "GET /path HTTP/1.1" + CRLF
                + "Host: test.com" + CRLF
                + "Connection: close" + CRLF
                + CRLF;

        assertEquals(expectedRequest, mOutputStream.toString());
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
    }

    @After
    public void tearDown() throws IOException {
        mOutputStream.close();
        mInputStream.close();
    }
    
    @Test
    public void executeStreamRequestTest() throws IOException {
        // Prepare HTTP response with headers and body
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: 16\r\n" +
                "\r\n" +
                "{\"data\":\"test\"}";
        
        // Set up input stream with the HTTP response
        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpResponse.getBytes());
        when(mSocket.getInputStream()).thenReturn(inputStream);
        
        // Execute the stream request
        URL url = new URL("https://test.com/stream");
        HttpStreamResponse response = mExecutor.executeStreamRequest(
                mSocket,     // finalSocket
                mSocket,     // tunnelSocket (using same mock for simplicity)
                null,        // originSocket
                url,
                HttpMethod.GET,
                Collections.emptyMap(),
                null         // serverCertificates
        );
        
        // Verify the request was sent correctly
        String expectedRequest = "GET /stream HTTP/1.1\r\n" +
                "Host: test.com\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        assertEquals(expectedRequest, mOutputStream.toString());
        
        // Verify the response properties
        assertNotNull(response);
        assertEquals(200, response.getHttpStatus());
        
        // Verify we can read from the response stream
        BufferedReader reader = response.getBufferedReader();
        assertNotNull(reader);
        StringBuilder responseBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBody.append(line);
        }
        assertEquals("{\"data\":\"test\"}", responseBody.toString());
        
        // Close the response
        response.close();
    }
    
    @Test(expected = IOException.class)
    public void executeStreamRequestWithSocketException() throws IOException {
        URL url = new URL("http://test.com/stream");
        when(mSocket.getOutputStream()).thenThrow(new IOException("Socket error"));
        
        mExecutor.executeStreamRequest(
                mSocket,
                mSocket,
                null,
                url,
                HttpMethod.GET,
                Collections.emptyMap(),
                null
        );
    }
}
