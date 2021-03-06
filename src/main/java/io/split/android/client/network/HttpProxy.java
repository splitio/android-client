package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static androidx.core.util.Preconditions.checkNotNull;

public class HttpProxy {

    final private String host;
    final private int port;
    final private String username;
    final private String password;

    public HttpProxy(@NonNull String host, int port) {
        this(host, port, null, null);
    }

    public HttpProxy(@NonNull String host, int port, @Nullable String username, @Nullable String password) {
        checkNotNull(host);

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean usesCredentials() {
        return username == null;
    }
}
