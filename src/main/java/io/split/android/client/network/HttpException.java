package io.split.android.client.network;

public class HttpException extends Exception {
    public HttpException(String message) {
        super("HttpException: " + message);
    }
}
