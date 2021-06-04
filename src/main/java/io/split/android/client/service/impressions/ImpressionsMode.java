package io.split.android.client.service.impressions;

public enum ImpressionsMode {
    OPTIMIZED,
    DEBUG;

    public static ImpressionsMode fromString(String value) {
        return "DEBUG".equals(value) ? DEBUG : OPTIMIZED;
    }
}
