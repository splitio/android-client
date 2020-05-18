package io.split.android.client.network;

import androidx.annotation.Nullable;

public class HttpProxy {
    final private String address;
    final private int port;
    final private String username;
    final private String password;

    public HttpProxy(String address, int port) {
        this(address, port, null, null);
    }

    public HttpProxy(String address, int port, @Nullable String username, @Nullable String password) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getAddress() {
        return address;
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
