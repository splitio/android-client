package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.InputStream;

public class HttpProxy {

    public enum ProxyAuthType {
        NONE,
        BASIC,
        BEARER,
        PROXY_CACERT, // --proxy-cacert
        MTLS
    }

    private final @NonNull String mHost;
    private final int mPort;
    private final @Nullable String mUsername;
    private final @Nullable String mPassword;
    private final @NonNull ProxyAuthType mAuthType;
    private final @Nullable String mBearerToken;
    private final @Nullable InputStream mClientCertStream;
    private final @Nullable InputStream mClientKeyStream;
    private final @Nullable InputStream mCaCertStream;

    private HttpProxy(Builder builder) {
        mHost = builder.mHost;
        mPort = builder.mPort;
        mUsername = builder.mUsername;
        mPassword = builder.mPassword;
        mAuthType = builder.mAuthType;
        mBearerToken = builder.mBearerToken;
        mClientCertStream = builder.mClientCertStream;
        mClientKeyStream = builder.mClientKeyStream;
        mCaCertStream = builder.mCaCertStream;
    }

    public @NonNull String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public @Nullable String getUsername() {
        return mUsername;
    }

    public @Nullable String getPassword() {
        return mPassword;
    }

    public @NonNull ProxyAuthType getAuthType() {
        return mAuthType;
    }

    public @Nullable String getBearerToken() {
        return mBearerToken;
    }

    public @Nullable InputStream getClientCertStream() {
        return mClientCertStream;
    }

    public @Nullable InputStream getClientKeyStream() {
        return mClientKeyStream;
    }

    public @Nullable InputStream getCaCertStream() {
        return mCaCertStream;
    }

    /**
     * Returns true if this proxy uses any authentication method (not NONE).
     */
    public boolean usesCredentials() {
        return mAuthType != ProxyAuthType.NONE;
    }

    public static Builder newBuilder(@NonNull String host, int port) {
        return new Builder(host, port);
    }

    public static class Builder {
        private final @NonNull String mHost;
        private final int mPort;
        private @Nullable String mUsername;
        private @Nullable String mPassword;
        private @NonNull ProxyAuthType mAuthType = ProxyAuthType.NONE;
        private @Nullable String mBearerToken;
        private @Nullable String mClientPkcs12Password;
        private @Nullable InputStream mClientPkcs12Stream;
        private @Nullable InputStream mClientCertStream;
        private @Nullable InputStream mClientKeyStream;
        private @Nullable InputStream mCaCertStream;

        private Builder(@NonNull String host, int port) {
            checkNotNull(host);
            mHost = host;
            mPort = port;
        }

        /**
         * Configure HTTP or HTTPS proxy with Basic authentication (username and password).
         * Equivalent to curl's --proxy-user.
         *
         * @param username Proxy username
         * @param password Proxy password
         * @return this builder
         */
        public Builder basicAuth(@NonNull String username, @NonNull String password) {
            mUsername = username;
            mPassword = password;
            mAuthType = ProxyAuthType.BASIC;
            return this;
        }

        /**
         * Configure HTTP or HTTPS proxy with Bearer authentication (JWT or opaque token).
         * Sends 'Proxy-Authorization: Bearer <token>' header to the proxy.
         *
         * @param token Bearer token (JWT or similar)
         * @return this builder
         */
        public Builder bearerAuth(@NonNull String token) {
            mBearerToken = token;
            mAuthType = ProxyAuthType.BEARER;
            return this;
        }

        /**
         * Configure HTTPS proxy with a custom CA certificate for proxy trust validation.
         * Equivalent to curl's --proxy-cacert.
         *
         * @param caCertStream InputStream to CA certificate data (PEM or DER)
         * @return this builder
         */
        public Builder proxyCacert(@NonNull InputStream caCertStream) {
            mCaCertStream = caCertStream;
            mAuthType = ProxyAuthType.PROXY_CACERT;
            return this;
        }

        /**
         * Configure HTTPS proxy with mutual TLS (mTLS) using separate certificate and key files.
         * The client presents a certificate and key to the proxy, and optionally trusts a custom CA for the proxy.
         *
         * @param certStream InputStream to client certificate data (PEM or DER)
         * @param keyStream InputStream to client private key data (PEM format)
         * @param caCertStream InputStream to CA certificate for proxy trust validation
         * @return this builder
         */
        public Builder mtlsAuth(@NonNull InputStream certStream, @NonNull InputStream keyStream, @NonNull InputStream caCertStream) {
            mClientCertStream = certStream;
            mClientKeyStream = keyStream;
            mCaCertStream = caCertStream;
            mAuthType = ProxyAuthType.MTLS;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }
}
