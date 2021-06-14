package io.split.android.client.service.impressions;

public enum ImpressionsMode {
    OPTIMIZED,
    DEBUG;

    public static ImpressionsMode fromString(String value) {
        if (value != null) {
            value = value.toUpperCase();
        }
        return "DEBUG".equals(value) ? DEBUG : OPTIMIZED;
    }
}
