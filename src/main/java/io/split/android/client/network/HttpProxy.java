package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

public class HttpProxy {

    private final @NonNull String mHost;
    private final int mPort;
    private final @Nullable String mUsername;
    private final @Nullable String mPassword;
    private final @Nullable InputStream mClientCertStream;
    private final @Nullable InputStream mClientKeyStream;
    private final @Nullable InputStream mCaCertStream;
    private final @Nullable ProxyCredentialsProvider mCredentialsProvider;

    private HttpProxy(Builder builder) {
        mHost = builder.mHost;
        mPort = builder.mPort;
        mUsername = builder.mUsername;
        mPassword = builder.mPassword;
        mClientCertStream = builder.mClientCertStream;
        mClientKeyStream = builder.mClientKeyStream;
        mCaCertStream = builder.mCaCertStream;
        mCredentialsProvider = builder.mCredentialsProvider;
    }

    public @Nullable String getHost() {
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

    public @Nullable InputStream getClientCertStream() {
        return mClientCertStream;
    }

    public @Nullable InputStream getClientKeyStream() {
        return mClientKeyStream;
    }

    public @Nullable InputStream getCaCertStream() {
        return mCaCertStream;
    }

    public @Nullable ProxyCredentialsProvider getCredentialsProvider() {
        return mCredentialsProvider;
    }

    public static Builder newBuilder(@Nullable String host, int port) {
        return new Builder(host, port);
    }

    public static class Builder {
        private final @Nullable String mHost;
        private final int mPort;
        private @Nullable String mUsername;
        private @Nullable String mPassword;
        private @Nullable InputStream mClientCertStream;
        private @Nullable InputStream mClientKeyStream;
        private @Nullable InputStream mCaCertStream;
        @Nullable
        private ProxyCredentialsProvider mCredentialsProvider;

        private Builder(@Nullable String host, int port) {
            mHost = host;
            mPort = port;
        }

        public Builder basicAuth(@NonNull String username, @NonNull String password) {
            mUsername = username;
            mPassword = password;
            return this;
        }

        public Builder proxyCacert(@NonNull InputStream caCertStream) {
            mCaCertStream = caCertStream;
            return this;
        }

        public Builder mtlsAuth(@NonNull InputStream clientCertStream, @NonNull InputStream keyStream) {
            mClientCertStream = clientCertStream;
            mClientKeyStream = keyStream;
            return this;
        }

        public Builder credentialsProvider(@NonNull ProxyCredentialsProvider credentialsProvider) {
            mCredentialsProvider = credentialsProvider;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }
}
