package io.split.android.client.service.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class HttpGeneralException extends Exception {

    private final Integer mHttpStatus;

    public HttpGeneralException(String path, String message, @Nullable Integer httpStatus) {
        super(getMessage(path, message, httpStatus));
        mHttpStatus = httpStatus;
    }

    public HttpGeneralException(String path, String message) {
        super(getMessage(path, message, null));
        mHttpStatus = null;
    }

    @NonNull
    private static String getMessage(String path, String message, @Nullable Integer httpStatus) {
        return String.format("Error while sending data to %s: %s%s", path, message, (httpStatus != null) ? ". Http status: " + httpStatus : "");
    }

    @Nullable
    public Integer getHttpStatus() {
        return mHttpStatus;
    }
}
