package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HttpProxy {

    public enum ProxyAuthType {
        NONE,
        BASIC,
        DIGEST,
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
    private final @Nullable String mClientCertPath;
    private final @Nullable String mClientKeyPath; // for MTLS
    private final @Nullable String mCaCertPath;    // for MTLS (optional)


    private HttpProxy(Builder builder) {
        mHost = builder.mHost;
        mPort = builder.mPort;
        mUsername = builder.mUsername;
        mPassword = builder.mPassword;
        mAuthType = builder.mAuthType;
        mBearerToken = builder.mBearerToken;
        mClientCertPath = builder.mClientCertPath;
        mClientKeyPath = builder.mClientKeyPath;
        mCaCertPath = builder.mCaCertPath;
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

    public @Nullable String getClientCertPath() {
        return mClientCertPath;
    }

    public @Nullable String getClientKeyPath() {
        return mClientKeyPath;
    }

    public @Nullable String getCaCertPath() {
        return mCaCertPath;
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
        private @Nullable String mClientCertPath;
        private @Nullable String mClientKeyPath;
        private @Nullable String mCaCertPath;


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
         * Configure HTTP or HTTPS proxy with Digest authentication (username and password).
         * Equivalent to curl's --proxy-digest and --proxy-user.
         *
         * @param username Proxy username
         * @param password Proxy password
         * @return this builder
         */
        public Builder digestAuth(@NonNull String username, @NonNull String password) {
            mUsername = username;
            mPassword = password;
            mAuthType = ProxyAuthType.DIGEST;
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
         * @param caCertPath Path to CA certificate file (PEM or DER)
         * @return this builder
         */
        public Builder proxyCacert(@NonNull String caCertPath) {
            mCaCertPath = caCertPath;
            mAuthType = ProxyAuthType.PROXY_CACERT;
            return this;
        }

        /**
         * Configure HTTPS proxy with mutual TLS (mTLS).
         * The client presents a certificate and key to the proxy, and optionally trusts a custom CA for the proxy.
         * Equivalent to curl's --proxy-cert, --proxy-key, and --proxy-cacert.
         *
         * @param certPath Path to client certificate file (PEM or PKCS12)
         * @param keyPath Path to client private key file (PEM, if not bundled with cert)
         * @param caCertPath Optional path to CA certificate for proxy trust validation
         * @return this builder
         */
        public Builder mtlsAuth(@NonNull String certPath, @NonNull String keyPath, @Nullable String caCertPath) {
            mClientCertPath = certPath;
            mClientKeyPath = keyPath;
            mCaCertPath = caCertPath;
            mAuthType = ProxyAuthType.MTLS;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }
}
