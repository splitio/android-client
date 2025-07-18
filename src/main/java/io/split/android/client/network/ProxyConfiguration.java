package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.utils.logger.Logger;

/**
 * Proxy configuration
 */
public class ProxyConfiguration {

    private final URI mUrl;
    private final ProxyCredentialsProvider mCredentialsProvider;
    private final InputStream mClientCert;
    private final InputStream mClientPk;
    private final InputStream mCaCert;

    ProxyConfiguration(@NonNull URI url,
                       @Nullable ProxyCredentialsProvider credentialsProvider,
                       @Nullable InputStream clientCert,
                       @Nullable InputStream clientPk,
                       @Nullable InputStream caCert) {
        mUrl = url;
        mCredentialsProvider = credentialsProvider;
        mClientCert = clientCert;
        mClientPk = clientPk;
        mCaCert = caCert;
    }

    URI getUrl() {
        return mUrl;
    }

    ProxyCredentialsProvider getCredentialsProvider() {
        return mCredentialsProvider;
    }

    InputStream getClientCert() {
        return mClientCert;
    }

    InputStream getClientPk() {
        return mClientPk;
    }

    InputStream getCaCert() {
        return mCaCert;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI mUrl;
        private ProxyCredentialsProvider mCredentialsProvider;
        private InputStream mClientCert;
        private InputStream mClientPk;
        private InputStream mCaCert;

        private Builder() {

        }

        /**
         * Set the proxy URL
         *
         * @param url MUST NOT be null
         * @return this builder
         */
        public Builder url(@NonNull String url) {
            try {
                mUrl = new URI(url);
            } catch (NullPointerException | URISyntaxException e) {
                Logger.e("Proxy url was not a valid URL.");
            }
            return this;
        }

        /**
         * Set the credentials provider.
         * <p>
         * Can be an implementation of {@link BearerCredentialsProvider} or {@link BasicCredentialsProvider}
         *
         * @param credentialsProvider A non null credentials provider
         * @return this builder
         */
        public Builder credentialsProvider(@NonNull ProxyCredentialsProvider credentialsProvider) {
            mCredentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Set the client certificate and private key in PKCS#8 format
         *
         * @param clientCert The client certificate
         * @param clientPk   The client private key
         * @return this builder
         */
        public Builder mtls(@NonNull InputStream clientCert, @NonNull InputStream clientPk) {
            mClientCert = clientCert;
            mClientPk = clientPk;
            return this;
        }

        /**
         * Set the Proxy CA certificate
         *
         * @param caCert The CA certificate in PEM or DER format
         * @return this builder
         */
        public Builder caCert(@NonNull InputStream caCert) {
            mCaCert = caCert;
            return this;
        }

        /**
         * Build the proxy configuration.
         * This method will return null if the proxy URL is not set.
         *
         * @return The proxy configuration
         */
        @Nullable
        public ProxyConfiguration build() {
            if (mUrl == null) {
                Logger.w("Proxy configuration with no URL. This will prevent SplitFactory from working.");
            }

            return new ProxyConfiguration(mUrl, mCredentialsProvider, mClientCert, mClientPk, mCaCert);
        }
    }
}
