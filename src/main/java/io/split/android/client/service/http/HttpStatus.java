package io.split.android.client.service.http;

import androidx.annotation.Nullable;

public enum HttpStatus {

    URI_TOO_LONG(414, "URI Too Long"),
    INTERNAL_NON_RETRYABLE(9009, "Non retryable");

    private final int mCode;
    private final String mDescription;

    HttpStatus(int code, String description) {
        mCode = code;
        mDescription = description;
    }

    public int getCode() {
        return mCode;
    }

    public String getDescription() {
        return mDescription;
    }

    @Nullable
    public static HttpStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (HttpStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
