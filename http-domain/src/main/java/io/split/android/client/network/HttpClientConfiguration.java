package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class HttpClientConfiguration {

    private final long mConnectionTimeout;
    private final long mReadTimeout;
    @Nullable
    private final HttpProxy mProxy;
    @Nullable
    private final CertificatePinningConfiguration mCertificatePinningConfiguration;
    @Nullable
    private final DevelopmentSslConfig mDevelopmentSslConfig;
    @Nullable
    private final SplitAuthenticator mProxyAuthenticator;

    private HttpClientConfiguration(Builder builder) {
        mConnectionTimeout = builder.mConnectionTimeout;
        mReadTimeout = builder.mReadTimeout;
        mProxy = builder.mProxy;
        mCertificatePinningConfiguration = builder.mCertificatePinningConfiguration;
        mDevelopmentSslConfig = builder.mDevelopmentSslConfig;
        mProxyAuthenticator = builder.mProxyAuthenticator;
    }

    public long getConnectionTimeout() {
        return mConnectionTimeout;
    }

    public long getReadTimeout() {
        return mReadTimeout;
    }

    @Nullable
    public HttpProxy getProxy() {
        return mProxy;
    }

    @Nullable
    public CertificatePinningConfiguration getCertificatePinningConfiguration() {
        return mCertificatePinningConfiguration;
    }

    @Nullable
    public DevelopmentSslConfig getDevelopmentSslConfig() {
        return mDevelopmentSslConfig;
    }

    @Nullable
    public SplitAuthenticator getProxyAuthenticator() {
        return mProxyAuthenticator;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long mConnectionTimeout;
        private long mReadTimeout;
        @Nullable
        private HttpProxy mProxy;
        @Nullable
        private CertificatePinningConfiguration mCertificatePinningConfiguration;
        @Nullable
        private DevelopmentSslConfig mDevelopmentSslConfig;
        @Nullable
        private SplitAuthenticator mProxyAuthenticator;

        private Builder() {
        }

        /**
         * Sets the connection timeout in milliseconds.
         */
        @NonNull
        public Builder connectionTimeout(long connectionTimeout) {
            mConnectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * Sets the read timeout in milliseconds.
         */
        @NonNull
        public Builder readTimeout(long readTimeout) {
            mReadTimeout = readTimeout;
            return this;
        }

        /**
         * Sets the HTTP proxy configuration.
         */
        @NonNull
        public Builder proxy(@Nullable HttpProxy proxy) {
            mProxy = proxy;
            return this;
        }

        /**
         * Sets the certificate pinning configuration.
         */
        @NonNull
        public Builder certificatePinningConfiguration(@Nullable CertificatePinningConfiguration configuration) {
            mCertificatePinningConfiguration = configuration;
            return this;
        }

        /**
         * Sets the development SSL configuration.
         * <p>
         * This is intended for development/testing environments only.
         */
        @NonNull
        public Builder developmentSslConfig(@Nullable DevelopmentSslConfig developmentSslConfig) {
            mDevelopmentSslConfig = developmentSslConfig;
            return this;
        }

        /**
         * Sets the proxy authenticator.
         */
        @NonNull
        public Builder proxyAuthenticator(@Nullable SplitAuthenticator proxyAuthenticator) {
            mProxyAuthenticator = proxyAuthenticator;
            return this;
        }

        /**
         * Builds the configuration.
         */
        @NonNull
        public HttpClientConfiguration build() {
            return new HttpClientConfiguration(this);
        }
    }
}
