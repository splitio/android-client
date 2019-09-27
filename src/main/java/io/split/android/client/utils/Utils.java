package io.split.android.client.utils;

import com.google.common.base.Strings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.network.URIBuilder;


public class Utils {

    public static boolean isSplitServiceReachable(URI uri) {
        try {
            return Utils.isReachable(new URIBuilder(uri, "/api/version").build());
        } catch (URISyntaxException e) {
            Logger.e("URI mal formed. Reachability function fails ", e);
        }
        return false;
    }

    public static boolean isReachable(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        if (port == -1) {
            String scheme = uri.getScheme();
            if (scheme.equals("http")) {
                port = 80;
            } else if (scheme.equals("https")) {
                port = 443;
            } else {
                return false;
            }
        }

        return isReachable(host, port);
    }

    public static boolean isReachable(String host, int port ) {
        return isReachable(host, port, 1500);
    }

    // TCP/HTTP/DNS (depending on the port, 53=DNS, 80=HTTP, etc.)
    public static boolean isReachable(String host, int port, int timeoutMs ) {
        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(host, port);

            socket.connect(socketAddress, timeoutMs);
            socket.close();

            return true;
        } catch (IOException e) { return false; }
    }

    public static String sanitizeForFileName(String string) {
        if(string == null) {
            return "";
        }
        return string.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    private static String sanitizeForFolderName(String string) {
        if(string == null) {
            return "";
        }
        return string.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static String convertApiKeyToFolder(String apiKey) {
        final int SALT_LENGTH = 29;
        final String SALT_PREFIX = "$2a$10$";
        final String CHAR_TO_FILL_SALT = "A";
        String sanitizedApiKey = sanitizeForFolderName(apiKey);
        StringBuilder salt = new StringBuilder(SALT_PREFIX);
        if (sanitizedApiKey.length() >= SALT_LENGTH - SALT_PREFIX.length()) {
            salt.append(sanitizedApiKey.substring(0, SALT_LENGTH - SALT_PREFIX.length()));
        } else {
            salt.append(sanitizedApiKey);
            salt.append(Strings.repeat(CHAR_TO_FILL_SALT, (SALT_LENGTH - SALT_PREFIX.length()) - sanitizedApiKey.length()));
        }
        // Remove last end of strings
        String cleanedSalt = salt.toString().substring(0, 29);
        String hash = BCrypt.hashpw(sanitizedApiKey, cleanedSalt);

        return (hash != null ? sanitizeForFolderName(hash) : null);
    }
}
