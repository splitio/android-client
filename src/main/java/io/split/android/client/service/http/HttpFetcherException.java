package io.split.android.client.service.http;

public class HttpFetcherException extends Exception {
    HttpFetcherException(String path, String message) {
        super("Error while fetching data from source " + path + ": " + message);
    }
}
