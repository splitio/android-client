package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;

public class HttpProxy {

    static final private String ADDRESS_SEPARATOR = ":";
    final private String address;
    final private int port;
    final private String username;
    final private String password;

    public HttpProxy(@NonNull String proxyAddress) {
        this(proxyAddress, null, null);
    }

    public HttpProxy(@NonNull String proxyAddress, @Nullable String username, @Nullable String password) {
        checkNotNull(proxyAddress);

        String address = proxyAddress;
        int port = 0;
        String[] proxyComponents = proxyAddress.split(ADDRESS_SEPARATOR);
        if (proxyComponents.length > 1) {
            address = proxyComponents[0];
            try {
                port = Integer.valueOf(proxyComponents[1]);
            } catch (NumberFormatException e) {
                Logger.e("Proxy port not valid. Using 0");
            }
        }
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
