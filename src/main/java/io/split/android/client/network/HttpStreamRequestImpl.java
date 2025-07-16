package io.split.android.client.network;

import static io.split.android.client.network.HttpRequestHelper.checkPins;
import static io.split.android.client.utils.Utils.checkNotNull;

import static io.split.android.client.network.HttpRequestHelper.applySslConfig;
import static io.split.android.client.network.HttpRequestHelper.applyTimeouts;
import static io.split.android.client.network.HttpRequestHelper.openConnection;

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

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.utils.logger.Logger;

public class HttpStreamRequestImpl implements HttpStreamRequest {

    private static final int STREAMING_READ_TIMEOUT_IN_MILLISECONDS = 80000;

    private final URI mUri;
    private final HttpMethod mHttpMethod;
    private final Map<String, String> mHeaders;
    private final UrlSanitizer mUrlSanitizer;
    private HttpURLConnection mConnection;
    private BufferedReader mBufferedReader;
    @Nullable
    private final Proxy mProxy;
    @Nullable
    private final SplitUrlConnectionAuthenticator mProxyAuthenticator;
    private final long mConnectionTimeout;
    @Nullable
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    @Nullable
    private final SSLSocketFactory mSslSocketFactory;
    @Nullable
    private final CertificateChecker mCertificateChecker;
    private final AtomicBoolean mWasRetried = new AtomicBoolean(false);
    @Nullable
    private final HttpProxy mHttpProxy;
    @Nullable
    private final ProxyCredentialsProvider mProxyCredentialsProvider;

    HttpStreamRequestImpl(@NonNull URI uri,
                          @NonNull Map<String, String> headers,
                          @Nullable Proxy proxy,
                          @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                          long connectionTimeout,
                          @Nullable DevelopmentSslConfig developmentSslConfig,
                          @Nullable SSLSocketFactory sslSocketFactory,
                          @NonNull UrlSanitizer urlSanitizer,
                          @Nullable CertificateChecker certificateChecker,
                          @Nullable HttpProxy httpProxy,
                          @Nullable ProxyCredentialsProvider proxyCredentialsProvider) {
        mUri = checkNotNull(uri);
        mHttpMethod = HttpMethod.GET;
        mProxy = proxy;
        mUrlSanitizer = checkNotNull(urlSanitizer);
        mHeaders = new HashMap<>(checkNotNull(headers));
        mProxyAuthenticator = proxyAuthenticator;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mSslSocketFactory = sslSocketFactory;
        mCertificateChecker = certificateChecker;
        mHttpProxy = httpProxy;
        mProxyCredentialsProvider = proxyCredentialsProvider;
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
        try {
            Logger.d("Closing streaming connection");
            disconnect();
        } catch (Exception e) {
            Logger.d("Unknown error closing connection: " + e.getLocalizedMessage());
        } finally {
            if (mBufferedReader != null) {
                closeBufferedReader();
            }
            Logger.d("Streaming connection closed");
        }
    }

    private void closeBufferedReader() {
        try {
            mBufferedReader.close();
        } catch (Exception e) {
            Logger.d("Unknown error closing buffer: " + e.getLocalizedMessage());
        }
    }

    private HttpStreamResponse getRequest() throws HttpException {
        HttpStreamResponse response;
        try {
            mConnection = setUpConnection(false);
            response = buildResponse(mConnection);

            if (response.getHttpStatus() == HttpURLConnection.HTTP_PROXY_AUTH) {
                response = handleAuthentication(response);
            }
        } catch (MalformedURLException e) {
            disconnect();
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            disconnect();
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (SSLPeerUnverifiedException e) {
            disconnect();
            throw new HttpException("SSL peer not verified: " + e.getLocalizedMessage(), HttpStatus.INTERNAL_NON_RETRYABLE.getCode());
        } catch (IOException e) {
            disconnect();
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        }

        return response;
    }

    private HttpURLConnection setUpConnection(boolean useProxyAuthenticator) throws IOException {
        URL url = mUrlSanitizer.getUrl(mUri);
        if (url == null) {
            throw new IOException("Error parsing URL");
        }

        HttpURLConnection connection = openConnection(
                url,
                mProxy,
                mHttpProxy,
                mProxyAuthenticator,
                mHttpMethod,
                mHeaders,
                useProxyAuthenticator,
                mSslSocketFactory,
                mProxyCredentialsProvider,
                null
        );
        applyTimeouts(HttpStreamRequestImpl.STREAMING_READ_TIMEOUT_IN_MILLISECONDS, mConnectionTimeout, connection);
        applySslConfig(mSslSocketFactory, mDevelopmentSslConfig, connection);
        connection.connect();
        checkPins(connection, mCertificateChecker);

        return connection;
    }

    private HttpStreamResponse handleAuthentication(HttpStreamResponse response) throws HttpException {
        if (!mWasRetried.getAndSet(true)) {
            try {
                Logger.d("Retrying with proxy authentication");
                setUpConnection(true);
                response = buildResponse(mConnection);
            } catch (Exception ex) {
                throw new HttpException("Something happened while retrieving data: " + ex.getLocalizedMessage());
            }
        }
        return response;
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

    private void disconnect() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }
}
