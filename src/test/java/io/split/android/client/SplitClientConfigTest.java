package io.split.android.client;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import io.split.android.client.utils.logger.LogPrinter;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class SplitClientConfigTest {

    @Test
    public void cannotSetFeatureRefreshRateToLessThan30() {
        SplitClientConfig build = SplitClientConfig.builder()
                .featuresRefreshRate(29)
                .build();

        assertFalse(build.featuresRefreshRate() == 29);
    }

    @Test
    public void cannotSetSegmentRefreshRateToLessThan30() {
        SplitClientConfig build = SplitClientConfig.builder()
                .segmentsRefreshRate(29)
                .build();

        assertFalse(build.segmentsRefreshRate() == 29);
    }

    @Test
    public void cannotSetImpressionRefreshRateToLessThan30() {
        SplitClientConfig build = SplitClientConfig.builder()
                .impressionsRefreshRate(29)
                .build();

        assertFalse(build.impressionsRefreshRate() == 29);
    }

    @Test
    public void canSetRefreshRatesTo30() {
        SplitClientConfig build = SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .build();

        assertEquals(30, build.featuresRefreshRate());
    }

    @Test
    public void telemetryRefreshRateLessThan60SetsValueToDefault() {
        SplitClientConfig config = SplitClientConfig.builder()
                .telemetryRefreshRate(30)
                .build();

        assertEquals(3600, config.telemetryRefreshRate());
    }

    @Test
    public void telemetryRefreshRateGreaterThan60IsAccepted() {
        SplitClientConfig config = SplitClientConfig.builder()
                .telemetryRefreshRate(120)
                .build();

        assertEquals(120, config.telemetryRefreshRate());
    }

    @Test
    public void logMessageIsDisplayedWhenUsingInvalidPrefix() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .prefix("")
                .build();

        assertEquals(2, logMessages.size());
        assertEquals("You passed an empty prefix, prefix must be a non-empty string", logMessages.poll());
        assertEquals("Setting prefix to empty string", logMessages.poll());
    }

    @Test
    public void logMessageIsDisplayedWhenUsingNullPrefix() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .prefix(null)
                .build();

        assertEquals(2, logMessages.size());
        assertEquals("You passed an empty prefix, prefix must be a non-empty string", logMessages.poll());
        assertEquals("Setting prefix to empty string", logMessages.poll());
    }

    @Test
    public void logMessageIsDisplayedWhenUsingEmptyPrefix() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .prefix(" ")
                .build();

        assertEquals(2, logMessages.size());
        assertEquals("You passed an empty prefix, prefix must be a non-empty string", logMessages.poll());
        assertEquals("Setting prefix to empty string", logMessages.poll());
    }

    @Test
    public void logMessageIsNotDisplayedWhenUsingValidPrefix() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .prefix("valid10_")
                .build();

        assertEquals(0, logMessages.size());
    }

    @NonNull
    private static Queue<String> getLogMessagesQueue() {
        Queue<String> logMessages = new LinkedList<>();
        Logger.instance().setPrinter(new LogPrinter() {
            @Override
            public void v(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }

            @Override
            public void d(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }

            @Override
            public void i(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }

            @Override
            public void w(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }

            @Override
            public void e(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }

            @Override
            public void wtf(String tag, String msg, Throwable tr) {
                logMessages.add(msg);
            }
        });
        return logMessages;
    }
}
