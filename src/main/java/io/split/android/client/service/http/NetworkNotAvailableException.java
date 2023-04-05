package io.split.android.client.service.http;

import androidx.annotation.Nullable;

public class NetworkNotAvailableException extends HttpFetcherException {

    public NetworkNotAvailableException(String path, String message, @Nullable Integer httpStatus) {
        super(path, message, httpStatus);
    }

    public NetworkNotAvailableException(String path, String message) {
        super(path, message);
    }
}
