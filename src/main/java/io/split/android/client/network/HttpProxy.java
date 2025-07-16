package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

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

    private HttpProxy(Builder builder) {
        mHost = builder.mHost;
        mPort = builder.mPort;
        mUsername = builder.mUsername;
        mPassword = builder.mPassword;
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

    public @Nullable InputStream getClientCertStream() {
        return mClientCertStream;
    }

    public @Nullable InputStream getClientKeyStream() {
        return mClientKeyStream;
    }

    public @Nullable InputStream getCaCertStream() {
        return mCaCertStream;
    }

    public static Builder newBuilder(@NonNull String host, int port) {
        return new Builder(host, port);
    }

    public static class Builder {
        private final @NonNull String mHost;
        private final int mPort;
        private @Nullable String mUsername;
        private @Nullable String mPassword;
        private @Nullable InputStream mClientCertStream;
        private @Nullable InputStream mClientKeyStream;
        private @Nullable InputStream mCaCertStream;

        private Builder(@NonNull String host, int port) {
            checkNotNull(host);
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

        public Builder mtlsAuth(@NonNull InputStream certStream, @NonNull InputStream keyStream, @NonNull InputStream caCertStream) {
            mClientCertStream = certStream;
            mClientKeyStream = keyStream;
            mCaCertStream = caCertStream;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }
}
