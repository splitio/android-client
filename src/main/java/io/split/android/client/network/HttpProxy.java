package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.InputStream;

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
    private final @Nullable String mClientPkcs12Password;
    private final @Nullable InputStream mClientPkcs12Stream;
    private final @Nullable InputStream mCaCertStream;

    private HttpProxy(Builder builder) {
        mHost = builder.mHost;
        mPort = builder.mPort;
        mUsername = builder.mUsername;
        mPassword = builder.mPassword;
        mAuthType = builder.mAuthType;
        mBearerToken = builder.mBearerToken;
        mClientPkcs12Password = builder.mClientPkcs12Password;
        mClientPkcs12Stream = builder.mClientPkcs12Stream;
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

    public @Nullable String getClientPkcs12Password() {
        return mClientPkcs12Password;
    }

    public @Nullable InputStream getClientPkcs12Stream() {
        return mClientPkcs12Stream;
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
         * @param caCertStream InputStream to CA certificate data (PEM or DER)
         * @return this builder
         */
        public Builder proxyCacert(@NonNull InputStream caCertStream) {
            mCaCertStream = caCertStream;
            mAuthType = ProxyAuthType.PROXY_CACERT;
            return this;
        }

        /**
         * Configure HTTPS proxy with mutual TLS (mTLS).
         * The client presents a certificate and key to the proxy, and optionally trusts a custom CA for the proxy.
         * Equivalent to curl's --proxy-cert, --proxy-key, and --proxy-cacert.
         *
         * @param pkcs12Stream InputStream to client PKCS#12 data
         * @param pkcs12Password Password for the PKCS#12 file
         * @param caCertStream Optional InputStream to CA certificate for proxy trust validation
         * @return this builder
         */
        public Builder mtlsAuth(@NonNull InputStream pkcs12Stream, @NonNull String pkcs12Password, @Nullable InputStream caCertStream) {
            mClientPkcs12Stream = pkcs12Stream;
            mClientPkcs12Password = pkcs12Password;
            mCaCertStream = caCertStream;
            mAuthType = ProxyAuthType.MTLS;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }
}
