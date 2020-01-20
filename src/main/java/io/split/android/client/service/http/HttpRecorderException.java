package io.split.android.client.service.http;

public class HttpRecorderException extends Exception {
    public HttpRecorderException(String path, String message) {
        super("Error while sending data to " + path + ": " + message);
    }
}