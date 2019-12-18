package io.split.android.client.service;

public class HttpRecorderException extends Exception {
    HttpRecorderException(String path, String message) {
        super("Error while sending data to " + path + ": " + message);
    }
}