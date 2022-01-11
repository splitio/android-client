package io.split.android.client.service.http;

import androidx.annotation.Nullable;

public class HttpRecorderException extends HttpGeneralException {

    public HttpRecorderException(String path, String message, @Nullable Integer httpStatus) {
        super(path, message, httpStatus);
    }

    public HttpRecorderException(String path, String message) {
        super(path, message);
    }
}
