package io.split.android.client.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Strings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.logger.Logger;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class HttpClientImpl implements HttpClient {
    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    private static final long STREAMING_READ_TIMEOUT_IN_MILLISECONDS = 80000;

    private final OkHttpClient mOkHttpClient;
    private final OkHttpClient mOkHttpClientStreaming;
    private final Map<String, String> mCommonHeaders;
    private final Map<String, String> mStreamingHeaders;

    private HttpClientImpl(OkHttpClient okHttpClient, OkHttpClient okHttpClientStreaming) {
        mCommonHeaders = new HashMap<>();
        mStreamingHeaders = new HashMap<>();
        mOkHttpClient = okHttpClient;
        mOkHttpClientStreaming = okHttpClientStreaming;
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body, Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>(mCommonHeaders);
        if (headers != null) {
            newHeaders.putAll(headers);
        }

        return new HttpRequestImpl(mOkHttpClient, uri, requestMethod, body, newHeaders);
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
        return new HttpStreamRequestImpl(mOkHttpClientStreaming, uri, mStreamingHeaders);
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
        mOkHttpClient.connectionPool().evictAll();
        mOkHttpClientStreaming.connectionPool().evictAll();
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
            Proxy proxy = null;
            okhttp3.Authenticator proxyAuthenticator = null;
            if (mProxy != null) {
                proxy = createProxy(mProxy);
                if (mProxyAuthenticator != null) {
                    proxyAuthenticator = createAuthenticator(mProxyAuthenticator);
                } else if (!Strings.isNullOrEmpty(mProxy.getUsername())) {
                    proxyAuthenticator = createBasicAuthenticator(mProxy.getUsername(), mProxy.getPassword());
                }
            }

            // Avoiding newBuilder on purpose to use different thread pool and resources
            return new HttpClientImpl(
                    createOkHttpClient(proxy, proxyAuthenticator, readTimeout, connectionTimeout, developmentSslConfig, mHostAppContext),
                    createOkHttpClient(proxy, proxyAuthenticator, STREAMING_READ_TIMEOUT_IN_MILLISECONDS,
                            connectionTimeout, developmentSslConfig, mHostAppContext)
            );
        }

        private OkHttpClient createOkHttpClient(Proxy proxy,
                                                okhttp3.Authenticator proxyAuthenticator,
                                                Long readTimeout,
                                                Long connectionTimeout,
                                                DevelopmentSslConfig developmentSslConfig,
                                                Context context) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (proxy != null) {
                builder.proxy(proxy);
            }

            if (proxyAuthenticator != null) {
                Logger.v("Setting up proxy authenticator");
                builder.proxyAuthenticator(proxyAuthenticator);
            }

            if (readTimeout != null && readTimeout > 0) {
                builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            }

            if (connectionTimeout != null && connectionTimeout > 0) {
                builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
            }

            // Both options overrides SSLSocketFactory
            if (developmentSslConfig != null) {
                builder.sslSocketFactory(developmentSslConfig.getSslSocketFactory(), developmentSslConfig.getTrustManager());
                builder.hostnameVerifier(developmentSslConfig.getHostnameVerifier());
            } else if (LegacyTlsUpdater.couldBeOld()) {
                forceTls12OnOldAndroid(builder, context);
            }
            return builder.build();
        }

        private Proxy createProxy(HttpProxy proxy) {
            if (proxy == null) {
                return null;
            }
            return new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy.getHost(), proxy.getPort()));
        }

        private okhttp3.Authenticator createBasicAuthenticator(String username, String password) {
            return new okhttp3.Authenticator() {
                @Nullable
                @Override
                public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder().header(PROXY_AUTHORIZATION_HEADER, credential).build();
                }
            };
        }

        private okhttp3.Authenticator createAuthenticator(SplitAuthenticator authenticator) {
            return new SplitOkHttpAuthenticator(authenticator);
        }

        private void forceTls12OnOldAndroid(OkHttpClient.Builder okHttpBuilder, Context context) {

            LegacyTlsUpdater.update(context);
            try {
                Tls12OnlySocketFactory factory = new Tls12OnlySocketFactory();
                okHttpBuilder.sslSocketFactory(factory, factory.defaultTrustManager());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                Logger.e("TLS v12 algorithm not available: " + e.getLocalizedMessage());
            } catch (GeneralSecurityException e) {
                Logger.e("TLS v12 security error: " + e.getLocalizedMessage());
            } catch (Exception e) {
                Logger.e("Unknown TLS v12 error: " + e.getLocalizedMessage());
            }
        }
    }
}
