package io.split.android.client.service.http;

import androidx.annotation.Nullable;

public class HttpFetcherException extends HttpGeneralException {

    public HttpFetcherException(String path, String message, @Nullable Integer httpStatus) {
        super(path, message, httpStatus);
    }

    public HttpFetcherException(String path, String message) {
        super(path, message);
    }
}
