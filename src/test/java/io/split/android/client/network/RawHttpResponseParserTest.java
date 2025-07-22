package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Objects;

public class RawHttpResponseParserTest {

    private final Certificate[] mServerCertificates = new Certificate[]{};

    @Test
    public void httpResponseWithValidResponse() throws Exception {
        String rawHttpResponse =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: 25\r\n" +
            "\r\n" +
            "{\"message\":\"Hello World\"}";
        
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        HttpResponse response = parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);

        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 200", 200, response.getHttpStatus());
        assertEquals("Response data should match", "{\"message\":\"Hello World\"}", response.getData());
        assertTrue("Response should be successful", response.isSuccess());
    }

    @Test
    public void responseWithErrorStatusReturnsErrorResponse() throws Exception {
        String rawHttpResponse =
            "HTTP/1.1 500 Internal Server Error\r\n" +
            "Content-Type: text/plain\r\n" +
            "Content-Length: 13\r\n" +
            "\r\n" +
            "Server Error!";
        
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        HttpResponse response = parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);

        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 500", 500, response.getHttpStatus());
        assertEquals("Response data should match", "Server Error!", response.getData());
        assertFalse("Response should not be successful", response.isSuccess());
    }

    @Test
    public void responseWithNoContentLengthReadsUntilEnd() throws Exception {
        String rawHttpResponse =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/plain\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "This is response data\r\n" +
            "with multiple lines\r\n" +
            "until connection closes";
        
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        HttpResponse response = parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);

        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 200", 200, response.getHttpStatus());
        assertNotNull("Response data should not be null", response.getData());
        assertTrue("Response data should contain expected content", 
                   response.getData().contains("This is response data"));
        assertTrue("Response data should contain multiple lines", 
                   response.getData().contains("with multiple lines"));
    }

    @Test
    public void responseWithNoBodyReturnsEmptyData() throws Exception {
        String rawHttpResponse =
            "HTTP/1.1 204 No Content\r\n" +
            "Content-Length: 0\r\n" +
            "\r\n";
        
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        HttpResponse response = parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);

        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 204", 204, response.getHttpStatus());
        assertTrue("Response data should be null or empty",
                   response.getData() == null || response.getData().isEmpty());
    }

    @Test
    public void responseWithInvalidStatusLineThrowsException() throws Exception {
        String rawHttpResponse = "INVALID STATUS LINE\r\n\r\n";
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        try {
            parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);
            fail("Should have thrown exception for invalid status line");
        } catch (IOException e) {
            assertTrue("Exception should mention invalid status", 
                       Objects.requireNonNull(e.getMessage()).contains("Invalid HTTP status"));
        }
    }

    @Test
    public void responseWithEmptyStreamThrowsException() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        RawHttpResponseParser parser = new RawHttpResponseParser();

        try {
            parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);
            fail("Should have thrown exception for empty stream");
        } catch (IOException e) {
            assertTrue("Exception should mention no response", 
                       e.getMessage().contains("No HTTP response"));
        }
    }

    @Test
    public void responseWithChunkedEncodingHandlesCorrectly() throws Exception {
        String rawHttpResponse =
            // headers
                "HTTP/1.1 200 OK\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n" +
                // 1st chunk size
                "15\r\n" +
                // 1st chunk data
                "This is chunked data!" +
                "\r\n" +

                // 2nd chunk size
                "0\r\n" +
                "\r\n";
        
        InputStream inputStream = new ByteArrayInputStream(rawHttpResponse.getBytes("UTF-8"));
        RawHttpResponseParser parser = new RawHttpResponseParser();

        HttpResponse response = parser.parseHttpResponse(inputStream, mServerCertificates, isStreaming);

        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 200", 200, response.getHttpStatus());
        assertNotNull("Response data should not be null", response.getData());
        assertTrue("Response data should contain expected content",
                   response.getData().contains("This is chunked data!"));
    }
}
