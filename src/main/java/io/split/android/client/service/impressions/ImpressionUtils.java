package io.split.android.client.service.impressions;

public class ImpressionUtils {

    public static final long DEFAULT_TIME_INTERVAL_MS = 3600L * 1000L; // 1 hour

    public static long truncateTimeframe(long timestampInMs, long defaultTimeIntervalMs) {
        return timestampInMs - (timestampInMs % defaultTimeIntervalMs);
    }
}
