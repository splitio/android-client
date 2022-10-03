package io.split.android.client.service.impressions;

public enum ImpressionsMode {
    OPTIMIZED,
    DEBUG,
    NONE;

    public static ImpressionsMode fromString(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }

        if ("DEBUG".equals(value)) {
            return DEBUG;
        } else if ("NONE".equals(value)) {
            return NONE;
        } else {
            return OPTIMIZED;
        }
    }
}
