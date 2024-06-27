package io.split.android.client.service.impressions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ImpressionUtilsTest {

    @Test
    public void truncateTimeframe() {
        long timestampInMs = System.currentTimeMillis();
        assertCorrectTruncation(timestampInMs, TimeUnit.SECONDS.toMillis(3));
        assertCorrectTruncation(timestampInMs, TimeUnit.MINUTES.toMillis(36));
        assertCorrectTruncation(timestampInMs, TimeUnit.MINUTES.toMillis(80));
        assertCorrectTruncation(timestampInMs, TimeUnit.HOURS.toMillis(1));
        assertCorrectTruncation(timestampInMs, TimeUnit.DAYS.toMillis(2));
    }

    private static void assertCorrectTruncation(long timestampInMs, long timeIntervalMs) {
        long truncateOneSecond = ImpressionUtils.truncateTimeframe(timestampInMs, timeIntervalMs);

        long timeDifference = timestampInMs - truncateOneSecond;
        assertTrue(timeDifference < timeIntervalMs);
    }
}
