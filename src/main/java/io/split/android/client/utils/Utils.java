package io.split.android.client.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;

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

    public static boolean isReachable(String host, int port) {
        return isReachable(host, port, 1500);
    }

    // TCP/HTTP/DNS (depending on the port, 53=DNS, 80=HTTP, etc.)
    public static boolean isReachable(String host, int port, int timeoutMs) {
        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(host, port);

            socket.connect(socketAddress, timeoutMs);
            socket.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String sanitizeForFileName(String string) {
        if (string == null) {
            return "";
        }
        return string.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    private static String sanitizeForFolderName(String string) {
        if (string == null) {
            return "";
        }
        return string.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static String convertApiKeyToFolder(String apiKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(apiKey.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int index = 0; index < hash.length; index++) {
                if (index % 5 != 0) {  /* Drop every 5th byte */
                    sb.append(String.format("%02x", hash[index]));
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
