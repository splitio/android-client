package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

public class HttpResponseConnectionAdapterTest {

    @Mock
    private HttpResponse mMockResponse;

    @Mock
    private Certificate mMockCertificate;

    private URL mTestUrl;
    private Certificate[] mTestCertificates;
    private HttpResponseConnectionAdapter mAdapter;

    @Before
    public void setUp() throws MalformedURLException {
        mMockCertificate = mock(Certificate.class);
        mMockResponse = mock(HttpResponse.class);
        mTestUrl = new URL("https://example.com/test");
        mTestCertificates = new Certificate[]{mMockCertificate};
    }

    @Test
    public void responseCodeIsValueFromResponse() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals(200, mAdapter.getResponseCode());
    }

    @Test
    public void successfulResponse() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("OK", mAdapter.getResponseMessage());
    }

    @Test
    public void status400Response() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(400);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("Bad Request", mAdapter.getResponseMessage());
    }

    @Test
    public void status401Response() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(401);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("Unauthorized", mAdapter.getResponseMessage());
    }

    @Test
    public void status403Response() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(403);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("Forbidden", mAdapter.getResponseMessage());
    }

    @Test
    public void status404Response() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(404);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("Not Found", mAdapter.getResponseMessage());
    }

    @Test
    public void status500Response() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(500);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("Internal Server Error", mAdapter.getResponseMessage());
    }

    @Test
    public void statusUnknownResponse() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(418);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        assertEquals("HTTP 418", mAdapter.getResponseMessage());
    }

    @Test
    public void successfulInputStream() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        when(mMockResponse.getData()).thenReturn("test data");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        InputStream inputStream = mAdapter.getInputStream();
        assertNotNull(inputStream);
        
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        String result = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        assertEquals("test data", result);
    }

    @Test
    public void nullDataInputStream() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        when(mMockResponse.getData()).thenReturn(null);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        InputStream inputStream = mAdapter.getInputStream();
        assertNotNull(inputStream);
        
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        assertEquals(-1, bytesRead);
    }

    @Test(expected = IOException.class)
    public void inputStreamErrorStatusThrows() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(400);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        mAdapter.getInputStream();
    }

    @Test(expected = IOException.class)
    public void inputStream500Throws() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(500);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        mAdapter.getInputStream();
    }

    @Test
    public void status400ErrorStream() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(400);
        when(mMockResponse.getData()).thenReturn("error message");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        InputStream errorStream = mAdapter.getErrorStream();
        assertNotNull(errorStream);
        
        byte[] buffer = new byte[1024];
        int bytesRead = errorStream.read(buffer);
        String result = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        assertEquals("error message", result);
    }

    @Test
    public void errorStreamStatus500() throws IOException {
        when(mMockResponse.getHttpStatus()).thenReturn(500);
        when(mMockResponse.getData()).thenReturn(null);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        InputStream errorStream = mAdapter.getErrorStream();
        assertNotNull(errorStream);
        
        byte[] buffer = new byte[1024];
        int bytesRead = errorStream.read(buffer);
        assertEquals(-1, bytesRead); // Empty stream
    }

    @Test
    public void errorStreamIsNullForSuccessful() {
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        InputStream errorStream = mAdapter.getErrorStream();
        assertNull(errorStream);
    }

    @Test
    public void usingProxyIsAlwaysTrue() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        assertTrue(mAdapter.usingProxy()); // This is only used for Proxy
    }

    @Test
    public void getServerCertificatesReturnsCerts() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        Certificate[] certificates = mAdapter.getServerCertificates();
        assertSame(mTestCertificates, certificates);
        assertEquals(1, certificates.length);
        assertSame(mMockCertificate, certificates[0]);
    }

    @Test
    public void nullServerCertificates() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, null);
        
        Certificate[] certificates = mAdapter.getServerCertificates();
        assertNull(certificates);
    }

    @Test
    public void contentTypeIsJsonForJsonData() {
        when(mMockResponse.getData()).thenReturn("{\"key\": \"value\"}");
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getHeaderField("content-type");
        assertEquals("application/json; charset=utf-8", contentType);
    }

    @Test
    public void nullHeaderField() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String result = mAdapter.getHeaderField(null);
        assertNull(result);
    }

    @Test
    public void getHeaderIsCaseInsensitive() {
        when(mMockResponse.getData()).thenReturn("{\"key\": \"value\"}");
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType1 = mAdapter.getHeaderField("Content-Type");
        String contentType2 = mAdapter.getHeaderField("CONTENT-TYPE");
        String contentType3 = mAdapter.getHeaderField("content-type");
        
        assertEquals("application/json; charset=utf-8", contentType1);
        assertEquals("application/json; charset=utf-8", contentType2);
        assertEquals("application/json; charset=utf-8", contentType3);
    }

    @Test
    public void generatedHeaderFieldsCanBeRetrieved() throws IOException {
        when(mMockResponse.getData()).thenReturn("{\"test\": \"data\"}");
        when(mMockResponse.getHttpStatus()).thenReturn(200);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        Map<String, List<String>> headers = mAdapter.getHeaderFields();
        assertNotNull(headers);
        
        assertTrue(headers.containsKey("content-type"));
        assertEquals("application/json; charset=utf-8", headers.get("content-type").get(0));
        
        assertTrue(headers.containsKey("content-length"));
        assertEquals("16", headers.get("content-length").get(0));
        
        assertTrue(headers.containsKey("content-encoding"));
        assertEquals("utf-8", headers.get("content-encoding").get(0));
        
        assertTrue(headers.containsKey("status"));
        assertEquals("200 OK", headers.get("status").get(0));
    }

    @Test
    public void getContentLengthWithData() {
        when(mMockResponse.getData()).thenReturn("Hello World");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);

        long length = mAdapter.getContentLengthLong();
        assertEquals(11, length); // "Hello World" is 11 bytes
    }

    @Test
    public void getContentLengthWithNullData() {
        when(mMockResponse.getData()).thenReturn(null);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        long length = mAdapter.getContentLengthLong();
        assertEquals(0, length);
    }

    @Test
    public void getContentLengthEmptyData() {
        when(mMockResponse.getData()).thenReturn("");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        long length = mAdapter.getContentLengthLong();
        assertEquals(0, length);
    }

    @Test
    public void getContentTypeJsonData() {
        when(mMockResponse.getData()).thenReturn("{\"key\": \"value\"}");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getContentType();
        assertEquals("application/json; charset=utf-8", contentType);
    }

    @Test
    public void getContentTypeJsonArray() {
        when(mMockResponse.getData()).thenReturn("[{\"key\": \"value\"}]");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getContentType();
        assertEquals("application/json; charset=utf-8", contentType);
    }

    @Test
    public void getContentTypeHtmlData() {
        when(mMockResponse.getData()).thenReturn("<html><body>Test</body></html>");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getContentType();
        assertEquals("text/html; charset=utf-8", contentType);
    }

    @Test
    public void getContentTypeText() {
        when(mMockResponse.getData()).thenReturn("Plain text content");
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getContentType();
        assertEquals("text/plain; charset=utf-8", contentType);
    }

    @Test
    public void getContentTypeNullDataHasNoContentType() {
        when(mMockResponse.getData()).thenReturn(null);
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String contentType = mAdapter.getContentType();
        assertNull(contentType);
    }

    @Test
    public void testGetContentEncoding() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        String encoding = mAdapter.getContentEncoding();
        assertEquals("utf-8", encoding);
    }

    @Test
    public void testGetDate() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        long currentTime = System.currentTimeMillis();
        long date = mAdapter.getDate();
        
        // Should be close to current time (within 1 second)
        assertTrue(Math.abs(date - currentTime) < 1000);
    }

    @Test
    public void urlCanBeRetrieved() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        URL url = mAdapter.getURL();
        assertSame(mTestUrl, url);
    }

    @Test(expected = IOException.class)
    public void getOutputStreamThrowsWhenNotEnabled() throws IOException {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        // Should throw exception since doOutput is not enabled
        mAdapter.getOutputStream();
    }
    
    @Test
    public void setDoOutputEnablesOutput() {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        
        // Initially doOutput should be false
        assertEquals(false, mAdapter.getDoOutput());
        
        // After setting doOutput to true, getDoOutput should return true
        mAdapter.setDoOutput(true);
        assertEquals(true, mAdapter.getDoOutput());
    }
    
    @Test
    public void getOutputStreamAfterEnablingOutput() throws IOException {
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates);
        mAdapter.setDoOutput(true);
        
        assertNotNull("Output stream should not be null when doOutput is enabled", mAdapter.getOutputStream());
    }
    
    @Test
    public void writeToOutputStream() throws IOException {
        // Create a ByteArrayOutputStream to capture the written data
        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        
        // Use the constructor that accepts a custom OutputStream
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates, testOutputStream);
        mAdapter.setDoOutput(true);
        
        // Write test data to the output stream
        String testData = "Test output data";
        mAdapter.getOutputStream().write(testData.getBytes(StandardCharsets.UTF_8));
        
        // Verify that the data was written correctly
        assertEquals("Written data should match the input", testData, testOutputStream.toString(StandardCharsets.UTF_8.name()));
    }
    
    @Test
    public void disconnectClosesOutputStream() throws IOException {
        // Create a custom OutputStream that tracks if it's been closed
        TestOutputStream testOutputStream = new TestOutputStream();
        
        mAdapter = new HttpResponseConnectionAdapter(mTestUrl, mMockResponse, mTestCertificates, testOutputStream);
        mAdapter.setDoOutput(true);
        
        // Get the output stream and write some data
        mAdapter.getOutputStream().write("Test".getBytes(StandardCharsets.UTF_8));
        
        // Verify the stream is not closed yet
        assertFalse("Output stream should not be closed before disconnect", testOutputStream.isClosed());
        
        // Disconnect should close the output stream
        mAdapter.disconnect();
        
        // Verify the stream was closed
        assertTrue("Output stream should be closed after disconnect", testOutputStream.isClosed());
    }
    
    /**
     * Custom OutputStream implementation for testing that tracks if it's been closed.
     */
    private static class TestOutputStream extends ByteArrayOutputStream {
        private boolean mClosed = false;
        
        @Override
        public void close() throws IOException {
            super.close();
            mClosed = true;
        }
        
        public boolean isClosed() {
            return mClosed;
        }
    }
}
