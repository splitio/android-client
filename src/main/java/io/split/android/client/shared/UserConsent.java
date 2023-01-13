package io.split.android.client.shared;

import androidx.annotation.Nullable;

import io.split.android.client.utils.logger.Logger;

public enum UserConsent {
    GRANTED,
    DECLINED,
    UNKNOWN;

    private static final String GRANTED_STRING = "GRANTED";
    private static final String DECLINED_STRING = "DECLINED";
    private static final String UNKNOWN_STRING = "UNKNOWN";

    @Nullable
    public static UserConsent fromString(String value) {
        if (value == null) {
            return null;
        }

        if (UNKNOWN_STRING.equals(value)) {
            return UNKNOWN;
        } else if (DECLINED_STRING.equals(value)) {
            return DECLINED;
        } else if (GRANTED_STRING.equals(value)) {
            return GRANTED;
        }
        return null;
    }
}
