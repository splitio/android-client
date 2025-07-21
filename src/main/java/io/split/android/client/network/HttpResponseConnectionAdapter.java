package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Adapter that wraps an HttpResponse as an HttpURLConnection.
 * <p>
 * This is only used to adapt the response from request through the TLS tunnel.
 */
class HttpResponseConnectionAdapter extends HttpsURLConnection {

    private final HttpResponse mResponse;
    private final URL mUrl;
    private final Certificate[] mServerCertificates;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private InputStream mErrorStream;
    private boolean mDoOutput = false;

    /**
     * Creates an adapter that wraps an HttpResponse as an HttpURLConnection.
     *
     * @param url                The URL of the request
     * @param response           The HTTP response from the SSL proxy
     * @param serverCertificates The server certificates from the SSL connection
     */
    HttpResponseConnectionAdapter(@NonNull URL url,
                                  @NonNull HttpResponse response,
                                  Certificate[] serverCertificates) {
        this(url, response, serverCertificates, new ByteArrayOutputStream());
    }

    @VisibleForTesting
    HttpResponseConnectionAdapter(@NonNull URL url,
                                  @NonNull HttpResponse response,
                                  Certificate[] serverCertificates,
                                  @NonNull OutputStream outputStream) {
        this(url, response, serverCertificates, outputStream, null, null);
    }
    
    @VisibleForTesting
    HttpResponseConnectionAdapter(@NonNull URL url,
                                  @NonNull HttpResponse response,
                                  Certificate[] serverCertificates,
                                  @NonNull OutputStream outputStream,
                                  @Nullable InputStream inputStream,
                                  @Nullable InputStream errorStream) {
        super(url);
        mUrl = url;
        mResponse = response;
        mServerCertificates = serverCertificates;
        mOutputStream = outputStream;
        mInputStream = inputStream;
        mErrorStream = errorStream;
    }

    @Override
    public int getResponseCode() throws IOException {
        return mResponse.getHttpStatus();
    }

    @Override
    public String getResponseMessage() throws IOException {
        // Map common HTTP status codes to messages
        switch (mResponse.getHttpStatus()) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "HTTP " + mResponse.getHttpStatus();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (mResponse.getHttpStatus() >= 400) {
            throw new IOException("HTTP " + mResponse.getHttpStatus());
        }
        if (mInputStream == null) {
            String data = mResponse.getData();
            if (data == null) {
                data = "";
            }
            mInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        }
        return mInputStream;
    }

    @Override
    public InputStream getErrorStream() {
        if (mResponse.getHttpStatus() >= 400) {
            if (mErrorStream == null) {
                String data = mResponse.getData();
                if (data == null) {
                    data = "";
                }
                mErrorStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
            return mErrorStream;
        }
        return null;
    }

    @Override
    public void connect() throws IOException {
        // Already connected
    }

    @Override
    public boolean usingProxy() {
        return true;
    }

    @Override
    public void disconnect() {
        // Close output stream if it exists
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (IOException e) {
            // Ignore exception during disconnect
        }
        
        // Close input stream if it exists
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            // Ignore exception during disconnect
        }
        
        // Close error stream if it exists
        try {
            if (mErrorStream != null) {
                mErrorStream.close();
            }
        } catch (IOException e) {
            // Ignore exception during disconnect
        }
    }

    // Required abstract method implementations for HTTPS connection
    @Override
    public String getCipherSuite() {
        return null;
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return null;
    }

    @Override
    public Certificate[] getServerCertificates() {
        // Return the server certificates from the SSL connection
        return mServerCertificates;
    }

    // Minimal implementations for other required methods
    @Override
    public void setRequestMethod(String method) {
    }

    @Override
    public String getRequestMethod() {
        return "GET";
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return true;
    }

    @Override
    public void setDoOutput(boolean doOutput) {
        mDoOutput = doOutput;
    }

    @Override
    public boolean getDoOutput() {
        return mDoOutput;
    }

    @Override
    public void setDoInput(boolean doInput) {
    }

    @Override
    public boolean getDoInput() {
        return true;
    }

    @Override
    public void setUseCaches(boolean useCaches) {
    }

    @Override
    public boolean getUseCaches() {
        return false;
    }

    @Override
    public void setIfModifiedSince(long ifModifiedSince) {
    }

    @Override
    public long getIfModifiedSince() {
        return 0;
    }

    @Override
    public void setDefaultUseCaches(boolean defaultUseCaches) {
    }

    @Override
    public boolean getDefaultUseCaches() {
        return false;
    }

    @Override
    public void setRequestProperty(String key, String value) {
    }

    @Override
    public void addRequestProperty(String key, String value) {
    }

    @Override
    public String getRequestProperty(String key) {
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return null;
    }

    @Override
    public String getHeaderField(String name) {
        if (name == null) {
            return null;
        }
        Map<String, List<String>> headers = getHeaderFields();
        List<String> values = headers.get(name.toLowerCase());

        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> headers = new HashMap<>();

        // Add synthetic headers based on response data
        String contentType = getContentType();
        if (contentType != null) {
            headers.put("content-type", Collections.singletonList(contentType));
        }

        long contentLength = getContentLengthLong();
        if (contentLength >= 0) {
            headers.put("content-length", Collections.singletonList(String.valueOf(contentLength)));
        }

        String contentEncoding = getContentEncoding();
        if (contentEncoding != null) {
            headers.put("content-encoding", Collections.singletonList(contentEncoding));
        }

        try {
            headers.put("status", Collections.singletonList(getResponseCode() + " " + getResponseMessage()));
        } catch (IOException e) {
            // Ignore if we can't get response code
        }

        return headers;
    }

    @Override
    public int getHeaderFieldInt(String name, int defaultValue) {
        String value = getHeaderField(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }

    @Override
    public long getHeaderFieldDate(String name, long defaultValue) {
        // We don't have actual date headers
        if ("date".equalsIgnoreCase(name)) {
            return System.currentTimeMillis();
        }
        return defaultValue;
    }

    @Override
    public String getHeaderFieldKey(int n) {
        Map<String, List<String>> headers = getHeaderFields();
        if (n >= 0 && n < headers.size()) {
            return (String) headers.keySet().toArray()[n];
        }
        return null;
    }

    @Override
    public String getHeaderField(int n) {
        String key = getHeaderFieldKey(n);
        return key != null ? getHeaderField(key) : null;
    }

    @Override
    public long getContentLengthLong() {
        String data = mResponse.getData();
        if (data == null) {
            return 0;
        }
        return data.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public String getContentType() {
        // Try to detect content type from response data, default to JSON for API responses
        String data = mResponse.getData();
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        String trimmed = data.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "application/json; charset=utf-8";
        }
        if (trimmed.startsWith("<")) {
            return "text/html; charset=utf-8";
        }
        return "text/plain; charset=utf-8";
    }

    @Override
    public String getContentEncoding() {
        return "utf-8";
    }

    @Override
    public long getExpiration() {
        return 0;
    }

    @Override
    public long getDate() {
        return System.currentTimeMillis();
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public URL getURL() {
        return mUrl;
    }

    @Override
    public int getContentLength() {
        long length = getContentLengthLong();
        return length > Integer.MAX_VALUE ? -1 : (int) length;
    }

    @Override
    public Permission getPermission() throws IOException {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!mDoOutput) {
            throw new IOException("Output not enabled for this connection. Call setDoOutput(true) first.");
        }
        return mOutputStream;
    }

    @Override
    public void setConnectTimeout(int timeout) {
    }

    @Override
    public int getConnectTimeout() {
        return 0;
    }

    @Override
    public void setReadTimeout(int timeout) {
    }

    @Override
    public int getReadTimeout() {
        return 0;
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier v) {
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return null;
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sf) {
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return null;
    }
}
