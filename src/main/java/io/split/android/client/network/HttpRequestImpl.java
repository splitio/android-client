package io.split.android.client.network;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

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
    private final SplitUrlConnectionAuthenticator mProxyAuthenticator;
    private final long mReadTimeout;
    private final long mConnectionTimeout;
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    private final SSLSocketFactory mSslSocketFactory;

    HttpRequestImpl(@NonNull URI uri,
                    @NonNull HttpMethod httpMethod,
                    @Nullable String body,
                    @NonNull Map<String, String> headers,
                    @Nullable Proxy proxy,
                    @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                    long readTimeout,
                    long connectionTimeout,
                    @Nullable DevelopmentSslConfig developmentSslConfig,
                    SSLSocketFactory sslSocketFactory,
                    @NonNull UrlSanitizer urlSanitizer) {
        mUri = checkNotNull(uri);
        mHttpMethod = checkNotNull(httpMethod);
        mBody = body;
        mHeaders = new HashMap<>(checkNotNull(headers));
        mUrlSanitizer = checkNotNull(urlSanitizer);
        mProxy = proxy;
        mProxyAuthenticator = proxyAuthenticator;
        mReadTimeout = readTimeout;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public HttpResponse execute() throws HttpException {

        switch (mHttpMethod) {
            case GET:
                return getRequest();
            case POST: {
                try {
                    return postRequest();
                } catch (IOException e) {
                    throw new HttpException("Error while posting data: " + e.getLocalizedMessage());
                }
            }
            default:
                throw new IllegalArgumentException("Request HTTP Method not valid: " + mHttpMethod.name());
        }
    }

    private HttpResponse getRequest() throws HttpException {
        HttpResponse response;
        HttpURLConnection connection = null;
        try {
            connection = setUpConnection(mHttpMethod);
            response = buildResponse(connection);
        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response;
    }

    @NonNull
    private HttpURLConnection setUpConnection(HttpMethod method) throws IOException {
        URL url = mUrlSanitizer.getUrl(mUri);
        if (url == null) {
            throw new IOException("Error parsing URL");
        }

        HttpURLConnection connection;
        if (mProxy != null) {
            connection = (HttpURLConnection) url.openConnection(mProxy);
            if (mProxyAuthenticator != null) {
                connection = mProxyAuthenticator.authenticate(connection);
            }
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }

        if (mReadTimeout > 0) {
            if (mReadTimeout > Integer.MAX_VALUE) {
                connection.setReadTimeout(Integer.MAX_VALUE);
            } else {
                connection.setReadTimeout((int) mReadTimeout);
            }
        }

        if (mConnectionTimeout > 0) {
            if (mConnectionTimeout > Integer.MAX_VALUE) {
                connection.setReadTimeout(Integer.MAX_VALUE);
            } else {
                connection.setConnectTimeout((int) mConnectionTimeout);
            }
        }

        if (mSslSocketFactory != null) {
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
            } else {
                Logger.e("Failed to set SSL socket factory in stream request. Connection is not SSL");
            }
        }

        if (mDevelopmentSslConfig != null) {
            try {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(mDevelopmentSslConfig.getSslSocketFactory());
                    ((HttpsURLConnection) connection).setHostnameVerifier(mDevelopmentSslConfig.getHostnameVerifier());
                } else {
                    Logger.e("Failed to set SSL socket factory in stream request. Connection is not SSL");
                }
            } catch (Exception ex) {
                Logger.e("Could not set development SSL config: " + ex.getLocalizedMessage());
            }
        }

        connection.setRequestMethod(method.name());
        addHeaders(connection, mHeaders);

        return connection;
    }

    private HttpResponse postRequest() throws IOException {

        if (mBody == null) {
            throw new IOException("Json data is null");
        }

        HttpURLConnection connection = null;
        HttpResponse httpResponse;
        try {
            connection = (HttpURLConnection) setUpConnection(mHttpMethod);
            connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8);

            if (!mBody.trim().isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream bodyStream = connection.getOutputStream()) {
                    bodyStream.write(mBody.getBytes());
                    bodyStream.flush();
                }
            }
            httpResponse = buildResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return httpResponse;
    }

    private static void addHeaders(HttpURLConnection request, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry == null) {
                continue;
            }

            request.addRequestProperty(entry.getKey(), entry.getValue());
        }
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
