package io.split.android.client.utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.UnsupportedCharsetException;


import timber.log.Timber;

public class Utils {

    public static StringEntity toJsonEntity(Object obj) {
        String json = Json.toJson(obj);
        StringEntity entity = null;
        try {
            entity = new StringEntity(json, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Timber.e(e);
        }
        entity.setContentType("application/json");
        return entity;
    }


    public static void forceClose(CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            // ignore
        }
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
}
