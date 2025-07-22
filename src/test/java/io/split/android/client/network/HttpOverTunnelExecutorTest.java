package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.POST, headers, body, null, isStreaming);

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

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null, isStreaming);

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

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null, isStreaming);

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

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null, isStreaming);

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

        mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null, isStreaming);
    }

    @Test
    public void getRequest() throws IOException {
        URL url = new URL("http://test.com/path");

        HttpResponse response = mExecutor.executeRequest(mSocket, url, HttpMethod.GET, Collections.emptyMap(), null, null, isStreaming);

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
}
