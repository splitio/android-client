package io.split.android.client.service.impressions;

public class ImpressionUtils {

    public static long truncateTimeframe(long timestampInMs, long defaultTimeIntervalMs) {
        return timestampInMs - (timestampInMs % defaultTimeIntervalMs);
    }
}
