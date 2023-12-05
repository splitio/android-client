package io.split.android.client.network;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

public class HttpStreamRequestImpl implements HttpStreamRequest {

    private static final int STREAMING_READ_TIMEOUT_IN_MILLISECONDS = 80000;
    private final URI mUri;
    private final Map<String, String> mHeaders;
    private HttpURLConnection mConnection;
    private BufferedReader mBufferedReader;
    @Nullable
    private final Proxy mProxy;
    @Nullable
    private final SplitUrlConnectionAuthenticator mProxyAuthenticator;
    private final long mConnectionTimeout;
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    private final SSLSocketFactory mSslSocketFactory;
    private final AtomicBoolean mWasRetried = new AtomicBoolean(false);

    HttpStreamRequestImpl(@NonNull URI uri,
                          @NonNull Map<String, String> headers,
                          @Nullable Proxy proxy,
                          @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                          long connectionTimeout,
                          @Nullable DevelopmentSslConfig developmentSslConfig,
                          @Nullable SSLSocketFactory sslSocketFactory) {
        mUri = checkNotNull(uri);
        mHeaders = new HashMap<>(checkNotNull(headers));
        mProxy = proxy;
        mProxyAuthenticator = proxyAuthenticator;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public HttpStreamResponse execute() throws HttpException {
        return getRequest();
    }

    @Override
    public void addHeader(String name, String value) {
        mHeaders.put(name, value);
    }

    @Override
    public void close() {
        Logger.d("Closing streaming connection");
        if (mBufferedReader != null) {
            closeBufferedReader();
        }
        mConnection.disconnect();
        Logger.d("Streaming connection closed");
    }

    private void closeBufferedReader() {
        try {
            mBufferedReader.close();
        } catch (Exception e) {
            Logger.d("Unknown error closing buffer: " + e.getLocalizedMessage());
        }
    }

    private HttpStreamResponse getRequest() throws HttpException {
        URL url;
        HttpStreamResponse response = null;
        try {
            url = mUri.toURL();
            mConnection = openConnection(url, mProxy, mProxyAuthenticator, mHeaders, false);
            applyTimeouts(mConnection, STREAMING_READ_TIMEOUT_IN_MILLISECONDS, mConnectionTimeout);
            applySslConfig(mSslSocketFactory, mDevelopmentSslConfig, mConnection);
            response = buildResponse(mConnection);

            if (response.getHttpStatus() == HttpURLConnection.HTTP_PROXY_AUTH) {
                response = handleAuthentication(response, url);
            }
        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        } finally {
            if (mConnection != null) {
                mConnection.disconnect();
            }
        }
        return response;
    }

    private HttpStreamResponse handleAuthentication(HttpStreamResponse response, URL url) throws HttpException {
        if (!mWasRetried.getAndSet(true)) {
            try {
                Logger.d("Retrying with proxy authentication");
                mConnection = openConnection(url, mProxy, mProxyAuthenticator, mHeaders, true);
                applyTimeouts(mConnection, STREAMING_READ_TIMEOUT_IN_MILLISECONDS, mConnectionTimeout);
                applySslConfig(mSslSocketFactory, mDevelopmentSslConfig, mConnection);
                response = buildResponse(mConnection);
            } catch (Exception ex) {
                throw new HttpException("Something happened while retrieving data: " + ex.getLocalizedMessage());
            }
        }
        return response;
    }

    private static void applySslConfig(SSLSocketFactory sslSocketFactory, DevelopmentSslConfig developmentSslConfig, HttpURLConnection connection) {
        if (sslSocketFactory != null) {
            Logger.d("Setting  SSL socket factory in stream request");
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            } else {
                Logger.e("Failed to set SSL socket factory.");
            }
        }

        if (developmentSslConfig != null) {
            try {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(developmentSslConfig.getSslSocketFactory());
                    ((HttpsURLConnection) connection).setHostnameVerifier(developmentSslConfig.getHostnameVerifier());
                } else {
                    Logger.e("Failed to set SSL socket factory in stream request.");
                }
            } catch (Exception ex) {
                Logger.e("Could not set development SSL config: " + ex.getLocalizedMessage());
            }
        }
    }

    private static void applyTimeouts(HttpURLConnection connection, int readTimeout, long connectionTimeout) {
        connection.setReadTimeout(readTimeout);

        if (connectionTimeout > 0) {
            if (connectionTimeout > Integer.MAX_VALUE) {
                connection.setReadTimeout(Integer.MAX_VALUE);
            } else {
                connection.setConnectTimeout((int) connectionTimeout);
            }
        }
    }

    private HttpURLConnection openConnection(@NonNull URL url,
                                             @Nullable Proxy proxy,
                                             @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                                             @NonNull Map<String, String> headers,
                                             boolean useProxyAuthenticator) throws IOException {
        HttpURLConnection connection;
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
            if (useProxyAuthenticator && proxyAuthenticator != null) {
                connection = proxyAuthenticator.authenticate(connection);
            }
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        addHeaders(connection, headers);

        return connection;
    }

    private static void addHeaders(HttpURLConnection request, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry == null) {
                continue;
            }

            request.addRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private HttpStreamResponse buildResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                if (mBufferedReader != null) {
                    closeBufferedReader();
                }
                mBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                return new HttpStreamResponseImpl(responseCode, mBufferedReader);
            }
        }

        return new HttpStreamResponseImpl(responseCode);
    }
}
