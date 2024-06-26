package io.split.android.client.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;

public class HttpClientImpl implements HttpClient {

    private final Map<String, String> mCommonHeaders;
    private final Map<String, String> mStreamingHeaders;

    @Nullable
    private final Proxy mProxy;
    @Nullable
    private final SplitUrlConnectionAuthenticator mProxyAuthenticator;
    private final long mReadTimeout;
    private final long mConnectionTimeout;
    @Nullable
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    @Nullable
    private final SSLSocketFactory mSslSocketFactory;
    @NonNull
    private final UrlSanitizer mUrlSanitizer;
    @Nullable
    private final CertificateChecker mCertificateChecker;

    HttpClientImpl(@Nullable HttpProxy proxy,
                   @Nullable SplitAuthenticator proxyAuthenticator,
                   long readTimeout,
                   long connectionTimeout,
                   @Nullable DevelopmentSslConfig developmentSslConfig,
                   @Nullable SSLSocketFactory sslSocketFactory,
                   @NonNull UrlSanitizer urlSanitizer,
                   @Nullable CertificateChecker certificateChecker) {
        mProxy = initializeProxy(proxy);
        mProxyAuthenticator = initializeProxyAuthenticator(proxy, proxyAuthenticator);
        mReadTimeout = readTimeout;
        mConnectionTimeout = connectionTimeout;
        mDevelopmentSslConfig = developmentSslConfig;
        mCommonHeaders = new HashMap<>();
        mStreamingHeaders = new HashMap<>();
        mSslSocketFactory = sslSocketFactory;
        mUrlSanitizer = urlSanitizer;
        mCertificateChecker = certificateChecker;
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
                mDevelopmentSslConfig,
                mSslSocketFactory,
                mUrlSanitizer,
                mCertificateChecker);
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
        return new HttpStreamRequestImpl(uri,
                mStreamingHeaders,
                mProxy,
                mProxyAuthenticator,
                mConnectionTimeout,
                mDevelopmentSslConfig,
                mSslSocketFactory,
                mUrlSanitizer,
                mCertificateChecker);
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

    private Proxy initializeProxy(HttpProxy proxy) {
        if (proxy != null) {
            return new Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(proxy.getHost(), proxy.getPort()));
        }

        return null;
    }

    private SplitUrlConnectionAuthenticator initializeProxyAuthenticator(HttpProxy proxy, SplitAuthenticator proxyAuthenticator) {
        if (proxy == null) {
            return null;
        } else if (proxyAuthenticator != null) {
            return new SplitUrlConnectionAuthenticator(proxyAuthenticator);
        } else if (!Utils.isNullOrEmpty(proxy.getUsername())) {
            return createBasicAuthenticator(proxy.getUsername(), proxy.getPassword());
        }

        return null;
    }

    private static SplitUrlConnectionAuthenticator createBasicAuthenticator(String username, String password) {
        return new SplitUrlConnectionAuthenticator(new SplitBasicAuthenticator(username, password, new Base64Encoder() {

            @Override
            public String encode(String value) {
                return Base64Util.encode(value);
            }

            @Override
            public String encode(byte[] bytes) {
                return Base64Util.encode(bytes);
            }
        }));
    }

    public static class Builder {
        private SplitAuthenticator mProxyAuthenticator;
        private HttpProxy mProxy;
        private long mReadTimeout = -1;
        private long mConnectionTimeout = -1;
        private DevelopmentSslConfig mDevelopmentSslConfig = null;
        private SSLSocketFactory mSslSocketFactory = null;
        private Context mHostAppContext;
        private UrlSanitizer mUrlSanitizer;
        private CertificatePinningConfiguration mCertificatePinningConfiguration;
        private CertificateChecker mCertificateChecker;

        public Builder setContext(Context context) {
            mHostAppContext = context;
            return this;
        }

        public Builder setProxy(HttpProxy proxy) {
            mProxy = proxy;
            return this;
        }

        public Builder setProxyAuthenticator(SplitAuthenticator authenticator) {
            if (authenticator != null) {
                Logger.v("Setting up proxy authenticator");
            }
            mProxyAuthenticator = authenticator;
            return this;
        }

        public Builder setReadTimeout(long readTimeout) {
            if (readTimeout > 0) {
                mReadTimeout = readTimeout;
            }
            return this;
        }

        public Builder setConnectionTimeout(long connectionTimeout) {
            if (connectionTimeout > 0) {
                mConnectionTimeout = connectionTimeout;
            }
            return this;
        }

        public Builder setDevelopmentSslConfig(DevelopmentSslConfig developmentSslConfig) {
            mDevelopmentSslConfig = developmentSslConfig;
            return this;
        }

        public Builder setUrlSanitizer(UrlSanitizer urlSanitizer) {
            mUrlSanitizer = urlSanitizer;
            return this;
        }

        public Builder setCertificatePinningConfiguration(CertificatePinningConfiguration certificatePinningConfiguration) {
            mCertificatePinningConfiguration = certificatePinningConfiguration;
            return this;
        }

        @VisibleForTesting
        Builder setCertificateChecker(CertificateChecker certificateChecker) {
            mCertificateChecker = certificateChecker;
            return this;
        }

        public HttpClient build() {
            if (mDevelopmentSslConfig == null) {
                if (LegacyTlsUpdater.couldBeOld()) {
                    LegacyTlsUpdater.update(mHostAppContext);
                    try {
                        mSslSocketFactory = new Tls12OnlySocketFactory();
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        Logger.e("TLS v12 algorithm not available: " + e.getLocalizedMessage());
                    } catch (Exception e) {
                        Logger.e("Unknown TLS v12 error: " + e.getLocalizedMessage());
                    }
                }
            }

            CertificateChecker certificateChecker;
            if (mCertificateChecker == null) {
                if (mCertificatePinningConfiguration == null) {
                    certificateChecker = null;
                } else {
                    certificateChecker = new CertificateCheckerImpl(mCertificatePinningConfiguration,
                            (mDevelopmentSslConfig != null) ? mDevelopmentSslConfig.getTrustManager() : null);
                }
            } else {
                // this is only for tests
                certificateChecker = mCertificateChecker;
            }

            return new HttpClientImpl(
                    mProxy,
                    mProxyAuthenticator,
                    mReadTimeout,
                    mConnectionTimeout,
                    mDevelopmentSslConfig,
                    mSslSocketFactory,
                    (mUrlSanitizer == null) ? new UrlSanitizerImpl() : mUrlSanitizer,
                    certificateChecker);
        }
    }
}
