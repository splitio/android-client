package io.split.android.client.service.impressions;

public enum ImpressionsMode {
    OPTIMIZED,
    DEBUG,
    NONE;

    public static ImpressionsMode fromString(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }
        return "DEBUG".equals(value) ? DEBUG : OPTIMIZED;
    }
}
