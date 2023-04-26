package io.split.android.client.network;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpRequestImpl implements HttpRequest {

    private final URI mUri;
    private final String mBody;
    private final HttpMethod mHttpMethod;
    private final Map<String, String> mHeaders;
    private final UrlSanitizer mUrlSanitizer;
    private final HttpProxy mProxy;
    private final SplitAuthenticator mProxyAuthenticator;
    private final long mReadTimeout;
    private final long mConnectionTimeout;
    private final DevelopmentSslConfig mDevelopmentSslConfig;

    HttpRequestImpl(@NonNull URI uri,
                    @NonNull HttpMethod httpMethod,
                    @Nullable String body,
                    @NonNull Map<String, String> headers,
                    @Nullable HttpProxy proxy,
                    @Nullable SplitAuthenticator proxyAuthenticator,
                    long readTimeout,
                    long connectionTimeout,
                    @Nullable DevelopmentSslConfig developmentSslConfig) {
        mUri = checkNotNull(uri);
        mHttpMethod = checkNotNull(httpMethod);
        mBody = body;
        mHeaders = new HashMap<>(checkNotNull(headers));
        mUrlSanitizer = new UrlSanitizerImpl();
        mProxy = proxy;
        mProxyAuthenticator = proxyAuthenticator;
        mReadTimeout = readTimeout;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
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
                    throw new HttpException("Error serializing request body: " + e.getLocalizedMessage());
                }
            }
            default:
                throw new IllegalArgumentException("Request HTTP Method not valid: " + mHttpMethod.name());
        }
    }

    private HttpResponse getRequest() throws HttpException {
        HttpResponse response;
        try {
            URLConnection connection = setUpConnection(mHttpMethod.name());
            response = buildResponse((HttpURLConnection) connection);
            ((HttpURLConnection) connection).disconnect();
        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        }
        return response;
    }

    @NonNull
    private URLConnection setUpConnection(String method) throws IOException {
        URL url = mUrlSanitizer.getUrl(mUri);

        URLConnection connection;
        if (mProxy != null) {
            System.out.println("Using proxy: " + mProxy.getHost() + " with port" + mProxy.getPort());
            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(mProxy.getHost(), mProxy.getPort()));
            connection = url.openConnection(proxy);
        } else {
            connection = url.openConnection();
        }

        if (mProxyAuthenticator != null) {
            connection = mProxyAuthenticator.authenticate(connection);
        }

        if (mReadTimeout > 0) {
            connection.setReadTimeout((int) mReadTimeout);
        }

        if (mConnectionTimeout > 0) {
            connection.setConnectTimeout((int) mConnectionTimeout);
        }

        if (mDevelopmentSslConfig != null) {
            try {
                ((HttpsURLConnection) connection).setSSLSocketFactory(mDevelopmentSslConfig.getSslSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(mDevelopmentSslConfig.getHostnameVerifier());
            } catch (Exception ex) {

            }
        }
        ((HttpURLConnection) connection).setRequestMethod(method);
        addHeaders((HttpURLConnection) connection);
        return connection;
    }

    private HttpResponse postRequest() throws IOException {
        if (mBody == null) {
            throw new IOException("Json data is null");
        }

        HttpURLConnection connection = (HttpURLConnection) setUpConnection(mHttpMethod.name());
        if (!mBody.trim().isEmpty()) {
            connection.setDoOutput(true);
            try (OutputStream bodyStream = connection.getOutputStream()) {
                bodyStream.write(mBody.getBytes());
                bodyStream.flush();
            }
        }
        HttpResponse httpResponse = buildResponse(connection);
        connection.disconnect();

        return httpResponse;
    }

    private void addHeaders(HttpURLConnection connection) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private HttpResponse buildResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;
            StringBuilder responseData = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseData.append(inputLine);
            }
            in.close();

            return new HttpResponseImpl(responseCode, (responseData.length() > 0 ? responseData.toString() : null));
        }
        return new HttpResponseImpl(responseCode);
    }
}
