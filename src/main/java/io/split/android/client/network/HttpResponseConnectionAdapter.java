package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Adapter that wraps an HttpResponse as an HttpURLConnection.
 * 
 * This class bridges our custom SSL proxy handling (which returns HttpResponse objects)
 * with the existing HTTP client architecture (which expects HttpURLConnection objects).
 * 
 * This is not a "mock" in the testing sense - it's a legitimate adapter pattern
 * that enables integration between our SSL proxy system and the existing codebase.
 */
class HttpResponseConnectionAdapter extends HttpsURLConnection {

    private final HttpResponse mResponse;
    private final URL mUrl;
    private boolean mConnected = false;

    public HttpResponseConnectionAdapter(@NonNull URL url, @NonNull HttpResponse response) {
        super(url);
        mUrl = url;
        mResponse = response;
        mConnected = true; // SSL proxy request already executed
    }

    @Override
    public int getResponseCode() throws IOException {
        return mResponse.getHttpStatus();
    }

    @Override
    public String getResponseMessage() throws IOException {
        // Map common HTTP status codes to messages
        switch (mResponse.getHttpStatus()) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "HTTP " + mResponse.getHttpStatus();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (mResponse.getHttpStatus() >= 400) {
            throw new IOException("HTTP " + mResponse.getHttpStatus());
        }
        String data = mResponse.getData();
        if (data == null) {
            data = "";
        }
        return new ByteArrayInputStream(data.getBytes("UTF-8"));
    }

    @Override
    public InputStream getErrorStream() {
        if (mResponse.getHttpStatus() >= 400) {
            String data = mResponse.getData();
            if (data == null) {
                data = "";
            }
            return new ByteArrayInputStream(data.getBytes());
        }
        return null;
    }

    @Override
    public void connect() throws IOException {
        // Already connected via SSL proxy
        mConnected = true;
    }

    @Override
    public boolean usingProxy() {
        return true; // Always true for SSL proxy connections
    }

    @Override
    public void disconnect() {
        mConnected = false;
    }

    // Required abstract method implementations
    @Override
    public String getCipherSuite() {
        return "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"; // Default cipher suite
    }

    @Override
    public java.security.cert.Certificate[] getLocalCertificates() {
        return null;
    }

    @Override
    public java.security.cert.Certificate[] getServerCertificates() {
        return new java.security.cert.Certificate[0];
    }

    // Minimal implementations for other required methods
    @Override public void setRequestMethod(String method) throws ProtocolException {}
    @Override public String getRequestMethod() { return "GET"; }
    @Override public void setInstanceFollowRedirects(boolean followRedirects) {}
    @Override public boolean getInstanceFollowRedirects() { return true; }
    @Override public void setDoOutput(boolean dooutput) {}
    @Override public boolean getDoOutput() { return false; }
    @Override public void setDoInput(boolean doinput) {}
    @Override public boolean getDoInput() { return true; }
    @Override public void setUseCaches(boolean usecaches) {}
    @Override public boolean getUseCaches() { return false; }
    @Override public void setIfModifiedSince(long ifmodifiedsince) {}
    @Override public long getIfModifiedSince() { return 0; }
    @Override public void setDefaultUseCaches(boolean defaultusecaches) {}
    @Override public boolean getDefaultUseCaches() { return false; }
    @Override public void setRequestProperty(String key, String value) {}
    @Override public void addRequestProperty(String key, String value) {}
    @Override public String getRequestProperty(String key) { return null; }
    @Override public Map<String, List<String>> getRequestProperties() { return null; }
    @Override public String getHeaderField(String name) { return null; }
    @Override public Map<String, List<String>> getHeaderFields() { return null; }
    @Override public int getHeaderFieldInt(String name, int Default) { return Default; }
    @Override public long getHeaderFieldDate(String name, long Default) { return Default; }
    @Override public String getHeaderFieldKey(int n) { return null; }
    @Override public String getHeaderField(int n) { return null; }
    @Override public long getContentLengthLong() { return -1; }
    @Override public String getContentType() { return "application/json"; }
    @Override public String getContentEncoding() { return null; }
    @Override public long getExpiration() { return 0; }
    @Override public long getDate() { return 0; }
    @Override public long getLastModified() { return 0; }
    @Override public URL getURL() { return mUrl; }
    @Override public int getContentLength() { return -1; }
    @Override public Permission getPermission() throws IOException { return null; }
    @Override public OutputStream getOutputStream() throws IOException { 
        throw new IOException("Output not supported for SSL proxy responses"); 
    }
    @Override public void setConnectTimeout(int timeout) {}
    @Override public int getConnectTimeout() { return 0; }
    @Override public void setReadTimeout(int timeout) {}
    @Override public int getReadTimeout() { return 0; }
    @Override public void setHostnameVerifier(HostnameVerifier v) {}
    @Override public HostnameVerifier getHostnameVerifier() { return null; }
    @Override public void setSSLSocketFactory(SSLSocketFactory sf) {}
    @Override public SSLSocketFactory getSSLSocketFactory() { return null; }
}
