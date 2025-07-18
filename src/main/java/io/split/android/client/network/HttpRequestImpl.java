package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

import static io.split.android.client.network.HttpRequestHelper.applySslConfig;
import static io.split.android.client.network.HttpRequestHelper.applyTimeouts;
import static io.split.android.client.network.HttpRequestHelper.createConnection;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpRetryException;
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

public class HttpRequestImpl implements HttpRequest {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";

    private final URI mUri;
    private final String mBody;
    private final HttpMethod mHttpMethod;
    private final Map<String, String> mHeaders;
    private final UrlSanitizer mUrlSanitizer;
    @Nullable
    private final Proxy mProxy;
    @Nullable
    private final HttpProxy mHttpProxy;
    @Nullable
    private final SplitUrlConnectionAuthenticator mProxyAuthenticator;
    @Nullable
    private final ProxyCredentialsProvider mProxyCredentialsProvider;
    private final long mReadTimeout;
    private final long mConnectionTimeout;
    @Nullable
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    @Nullable
    private final SSLSocketFactory mSslSocketFactory;
    @Nullable
    private final CertificateChecker mCertificateChecker;

    HttpRequestImpl(@NonNull URI uri,
                    @NonNull HttpMethod httpMethod,
                    @Nullable String body,
                    @NonNull Map<String, String> headers,
                    @Nullable Proxy proxy,
                    @Nullable HttpProxy httpProxy,
                    @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                    @Nullable ProxyCredentialsProvider proxyCredentialsProvider,
                    long readTimeout,
                    long connectionTimeout,
                    @Nullable DevelopmentSslConfig developmentSslConfig,
                    @Nullable SSLSocketFactory sslSocketFactory,
                    @NonNull UrlSanitizer urlSanitizer,
                    @Nullable CertificateChecker certificateChecker) {
        mUri = checkNotNull(uri);
        mHttpMethod = checkNotNull(httpMethod);
        mBody = body;
        mUrlSanitizer = checkNotNull(urlSanitizer);
        mHeaders = new HashMap<>(checkNotNull(headers));
        mProxy = proxy;
        mHttpProxy = httpProxy;
        mProxyAuthenticator = proxyAuthenticator;
        mProxyCredentialsProvider = proxyCredentialsProvider;
        mReadTimeout = readTimeout;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mSslSocketFactory = sslSocketFactory;
        mCertificateChecker = certificateChecker;
    }

    @Override
    public HttpResponse execute() throws HttpException {
        AtomicBoolean wasRetried = new AtomicBoolean(false);

        switch (mHttpMethod) {
            case GET:
                return getRequest(wasRetried);
            case POST: {
                return postRequest(wasRetried);
            }
            default:
                throw new IllegalArgumentException("Request HTTP Method not valid: " + mHttpMethod.name());
        }
    }

    private HttpResponse getRequest(AtomicBoolean wasRetried) throws HttpException {
        HttpResponse response;
        HttpURLConnection connection = null;
        try {
            connection = setUpConnection(false);
            response = buildResponse(connection);

            if (response.getHttpStatus() == HttpURLConnection.HTTP_PROXY_AUTH) {
                response = handleProxyAuthentication(response, true, wasRetried);
            }
        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (SSLPeerUnverifiedException e) {
            throw new HttpException("SSL Peer Unverified: " + e.getLocalizedMessage(), HttpStatus.INTERNAL_NON_RETRYABLE.getCode());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    private HttpResponse postRequest(AtomicBoolean wasRetried) throws HttpException {
        if (mBody == null) {
            throw new HttpException("Json data is null");
        }

        HttpURLConnection connection = null;
        HttpResponse response;
        try {
            connection = setUpPostConnection(false);
            response = buildResponse(connection);

            if (response.getHttpStatus() == HttpURLConnection.HTTP_PROXY_AUTH) {
                response = handleProxyAuthentication(response, false, wasRetried);
            }
        } catch (SSLPeerUnverifiedException e) {
            throw new HttpException("SSL Peer Unverified: " + e.getLocalizedMessage(), HttpStatus.INTERNAL_NON_RETRYABLE.getCode());
        } catch (IOException e) {
            throw new HttpException("Something happened while posting data: " + e.getLocalizedMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    private HttpResponse handleProxyAuthentication(HttpResponse originalResponse, boolean isGet, AtomicBoolean wasRetried) throws HttpException {
        HttpURLConnection connection = null;
        if (!wasRetried.getAndSet(true)) {
            try {
                Logger.d("Retrying with proxy authentication");
                connection = (isGet) ? setUpConnection(true) : setUpPostConnection(true);
                return buildResponse(connection);
            } catch (IOException ex) {
                throw new HttpException("Something happened while retrieving data: " + ex.getLocalizedMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        return originalResponse;
    }

    private HttpURLConnection setUpPostConnection(boolean useProxyAuthenticator) throws IOException {
        return setUpConnection(useProxyAuthenticator);
    }

    @NonNull
    private HttpURLConnection setUpConnection(boolean authenticate) throws IOException {
        URL url = mUrlSanitizer.getUrl(mUri);
        if (url == null) {
            throw new IOException("Error parsing URL");
        }

        HttpURLConnection connection;
        try {
            connection = getConnection(authenticate, url);
        } catch (HttpRetryException e) {
            if (mProxyAuthenticator == null) {
                throw e;
            }
            connection = getConnection(authenticate, url);
        }
        applyTimeouts(mReadTimeout, mConnectionTimeout, connection);
        applySslConfig(mSslSocketFactory, mDevelopmentSslConfig, connection);

        if (mBody != null && !mBody.trim().isEmpty()) {
            connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8);
            connection.setDoOutput(true);
            try (OutputStream bodyStream = connection.getOutputStream()) {
                bodyStream.write(mBody.getBytes());
                bodyStream.flush();
            }
        }

        connection.connect();
        HttpRequestHelper.checkPins(connection, mCertificateChecker);

        return connection;
    }

    @NonNull
    private HttpURLConnection getConnection(boolean authenticate, URL url) throws IOException {
        return createConnection(
                url,
                mProxy,
                mHttpProxy,
                mProxyAuthenticator,
                mHttpMethod,
                mHeaders,
                authenticate,
                mSslSocketFactory,
                mProxyCredentialsProvider,
                mBody,
                false);
    }

    private static HttpResponse buildResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
            StringBuilder responseData = new StringBuilder();
            try (InputStream inputStream = connection.getInputStream()) {
                if (inputStream != null) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            responseData.append(inputLine);
                        }
                    }
                }
            }

            return new HttpResponseImpl(responseCode, (responseData.length() > 0 ? responseData.toString() : null));
        }

        return new HttpResponseImpl(responseCode);
    }
}
