package io.split.android.client.network;

import android.content.Context;

import com.google.common.base.Strings;

import java.net.URI;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class HttpClientImpl implements HttpClient {
    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";

    private final Map<String, String> mCommonHeaders;
    private final Map<String, String> mStreamingHeaders;
    private final HttpProxy mProxy;
    private final SplitAuthenticator mProxyAuthenticator;
    private final long mReadTimeout;
    private final long mConnectionTimeout;
    private final DevelopmentSslConfig mDevelopmentSslConfig;

    public HttpClientImpl(HttpProxy proxy,
                          SplitAuthenticator proxyAuthenticator,
                          long readTimeout,
                          long connectionTimeout,
                          DevelopmentSslConfig developmentSslConfig) {
        mProxy = proxy;
        mProxyAuthenticator = proxyAuthenticator;
        mReadTimeout = readTimeout;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mCommonHeaders = new HashMap<>();
        mStreamingHeaders = new HashMap<>();
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body, Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>(mCommonHeaders);
        if (headers != null) {
            newHeaders.putAll(headers);
        }

        return new HttpRequestImpl(
                uri,
                requestMethod,
                body,
                newHeaders,
                mProxy,
                mProxyAuthenticator,
                mReadTimeout,
                mConnectionTimeout,
                mDevelopmentSslConfig);
    }

    public HttpRequest request(URI uri, HttpMethod requestMethod) {
        return request(uri, requestMethod, null);
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body) {
        return request(uri, requestMethod, body, null);
    }

    @Override
    public HttpStreamRequest streamRequest(URI uri) {
        return new HttpStreamRequestImpl(uri, mStreamingHeaders);
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(String.format("Invalid value for header %s: %s", name, value));
        }
        mCommonHeaders.put(name, value);
    }

    @Override
    public void setStreamingHeader(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(String.format("Invalid value for streaming header %s: %s", name, value));
        }
        mStreamingHeaders.put(name, value);
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
    }

    @Override
    public void addStreamingHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            setStreamingHeader(header.getKey(), header.getValue());
        }
    }

    @Override
    public void close() {

    }

    public static class Builder {
        private SplitAuthenticator mProxyAuthenticator;
        private HttpProxy mProxy;
        private long readTimeout = -1;
        private long connectionTimeout = -1;
        private DevelopmentSslConfig developmentSslConfig = null;
        private Context mHostAppContext;

        public Builder setContext(Context context) {
            mHostAppContext = context;
            return this;
        }

        public Builder setProxy(HttpProxy proxy) {
            mProxy = proxy;
            return this;
        }

        public Builder setProxyAuthenticator(SplitAuthenticator authenticator) {
            mProxyAuthenticator = authenticator;
            return this;
        }

        public Builder setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setDevelopmentSslConfig(DevelopmentSslConfig developmentSslConfig) {
            this.developmentSslConfig = developmentSslConfig;
            return this;
        }

        public HttpClient build() {
            SplitAuthenticator proxyAuthenticator = null;
            if (mProxy != null) {
                if (mProxyAuthenticator != null) {
                    proxyAuthenticator = mProxyAuthenticator;
                } else if (!Strings.isNullOrEmpty(mProxy.getUsername())) {
                    proxyAuthenticator = createBasicAuthenticator(mProxy.getUsername(), mProxy.getPassword());
                }
            }

            // Avoiding newBuilder on purpose to use different thread pool and resources
            return createOkHttpClient(mProxy, proxyAuthenticator, readTimeout, connectionTimeout, developmentSslConfig, mHostAppContext);
        }

        private HttpClient createOkHttpClient(HttpProxy proxy,
                                              SplitAuthenticator proxyAuthenticator,
                                              Long readTimeout,
                                              Long connectionTimeout,
                                              DevelopmentSslConfig developmentSslConfig,
                                              Context context) {
            Builder builder = new Builder();
            if (proxy != null) {
                builder.setProxy(proxy);
            }

            if (proxyAuthenticator != null) {
                builder.setProxyAuthenticator(proxyAuthenticator);
            }

            if (readTimeout != null && readTimeout > 0) {
                builder.setReadTimeout(readTimeout/*, TimeUnit.MILLISECONDS*/);
            }

            if (connectionTimeout != null && connectionTimeout > 0) {
                builder.setConnectionTimeout(connectionTimeout/*, TimeUnit.MILLISECONDS*/);
            }

            // Both options overrides SSLSocketFactory
            if (developmentSslConfig != null) {
                builder.setDevelopmentSslConfig(developmentSslConfig);
            } else if (LegacyTlsUpdater.couldBeOld()) {
                forceTls12OnOldAndroid(builder, context);
            }
            return new HttpClientImpl(
                    builder.mProxy, builder.mProxyAuthenticator, builder.readTimeout, builder.connectionTimeout, builder.developmentSslConfig);
        }

        private SplitAuthenticator createBasicAuthenticator(String username, String password) {
            return new SplitAuthenticator() {
                @Override
                public URLConnection authenticate(URLConnection connection) {
                    String credential = basic(username, password);
                    connection.setRequestProperty(PROXY_AUTHORIZATION_HEADER, credential);

                    return connection;
                }
            };
        }

        private void forceTls12OnOldAndroid(HttpClientImpl.Builder builder, Context context) {

            LegacyTlsUpdater.update(context);
            try {
                Tls12OnlySocketFactory factory = new Tls12OnlySocketFactory();
//                okHttpBuilder.sslSocketFactory(factory, factory.defaultTrustManager());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                Logger.e("TLS v12 algorithm not available: " + e.getLocalizedMessage());
            } catch (Exception e) {
                Logger.e("Unknown TLS v12 error: " + e.getLocalizedMessage());
            }
        }

        private static String basic(String username, String password) {
            String usernameAndPassword = username + ":" + password;
            String encoded = Base64Util.encode(usernameAndPassword);
            return "Basic " + encoded;
        }
    }
}
