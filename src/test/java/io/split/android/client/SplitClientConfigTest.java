package io.split.android.client;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.split.android.client.fallback.FallbackTreatmentsConfiguration;
import io.split.android.client.network.CertificatePinningConfiguration;
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

    @Test
    public void certificatePinningConfigWithoutPinsIsIgnored() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig config = SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .certificatePinningConfiguration(CertificatePinningConfiguration.builder().build())
                .build();

        assertNull(config.certificatePinningConfiguration());
        assertEquals(1, logMessages.size());
        assertEquals("Certificate pinning configuration is empty. Disabling certificate pinning.", logMessages.poll());
    }

    @Test
    public void impressionsDedupeTimeIntervalCannotBeLessThanOneHour() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(3600)
                .logLevel(SplitLogLevel.WARNING)
                .build();

        assertEquals(TimeUnit.HOURS.toMillis(1), config.impressionsDedupeTimeInterval());
        assertEquals(1, logMessages.size());
        assertEquals("Time interval for impressions dedupe is out of bounds. Setting to default value.", logMessages.poll());
    }

    @Test
    public void impressionsDedupeTimeIntervalCannotBeGreaterThanOneDay() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(25))
                .logLevel(SplitLogLevel.WARNING)
                .build();

        assertEquals(TimeUnit.HOURS.toMillis(1), config.impressionsDedupeTimeInterval());
        assertEquals(1, logMessages.size());
        assertEquals("Time interval for impressions dedupe is out of bounds. Setting to default value.", logMessages.poll());
    }

    @Test
    public void impressionsDedupeTimeIntervalWithinBoundsIsAccepted() {
        Queue<String> logMessages = getLogMessagesQueue();

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(2))
                .logLevel(SplitLogLevel.WARNING)
                .build();

        assertEquals(TimeUnit.HOURS.toMillis(2), config.impressionsDedupeTimeInterval());
        assertEquals(0, logMessages.size());
    }

    @Test
    public void defaultImpressionsDedupeTimeIntervalIsOneHour() {
        SplitClientConfig config = SplitClientConfig.builder().build();

        assertEquals(TimeUnit.HOURS.toMillis(1), config.impressionsDedupeTimeInterval());
    }

    @Test
    public void defaultObserverCacheExpirationPeriodIs4Hours() {
        SplitClientConfig config = SplitClientConfig.builder().build();

        assertEquals(TimeUnit.HOURS.toMillis(4), config.observerCacheExpirationPeriod());
    }

    @Test
    public void observerCacheExpirationPeriodIs4HoursWhenDedupeTimeIntervalIsLessThan4Hours() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(3))
                .build();

        assertEquals(TimeUnit.HOURS.toMillis(4), config.observerCacheExpirationPeriod());
    }

    @Test
    public void observerCacheExpirationPeriodMatchesDedupeTimeIntervalWhenDedupeTimeIntervalIsGreaterThan4Hours() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(5))
                .build();
        SplitClientConfig config2 = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(24))
                .build();
        SplitClientConfig config3 = SplitClientConfig.builder()
                .impressionsDedupeTimeInterval(TimeUnit.HOURS.toMillis(25))
                .build();

        assertEquals(TimeUnit.HOURS.toMillis(5), config.observerCacheExpirationPeriod());
        assertEquals(TimeUnit.HOURS.toMillis(24), config2.observerCacheExpirationPeriod());
        assertEquals(TimeUnit.HOURS.toMillis(4), config3.observerCacheExpirationPeriod());
    }

    @Test
    public void rolloutCacheConfigurationDefaults() {
        RolloutCacheConfiguration config = SplitClientConfig.builder().build().rolloutCacheConfiguration();

        assertEquals(10, config.getExpirationDays());
        assertFalse(config.isClearOnInit());
    }

    @Test
    public void rolloutCacheConfigurationExpirationIsCorrectlySet() {
        RolloutCacheConfiguration config = SplitClientConfig.builder()
                .rolloutCacheConfiguration(RolloutCacheConfiguration.builder().expirationDays(1).clearOnInit(true).build())
                .build().rolloutCacheConfiguration();

        assertEquals(1, config.getExpirationDays());
        assertTrue(config.isClearOnInit());
    }

    @Test
    public void nullRolloutCacheConfigurationSetsDefault() {
        Queue<String> logMessages = getLogMessagesQueue();
        RolloutCacheConfiguration config = SplitClientConfig.builder()
                .logLevel(SplitLogLevel.WARNING)
                .rolloutCacheConfiguration(null)
                .build().rolloutCacheConfiguration();

        assertEquals(10, config.getExpirationDays());
        assertFalse(config.isClearOnInit());
        assertEquals(1, logMessages.size());
    }

    @Test
    public void fallbackTreatmentsIsNullByDefault() {
        SplitClientConfig config = SplitClientConfig.builder().build();
        assertNull(config.fallbackTreatments());
    }

    @Test
    public void fallbackTreatmentsAreCorrectlySet() {
        FallbackTreatmentsConfiguration byFactoryConfig = FallbackTreatmentsConfiguration.builder().build();
        FallbackTreatmentsConfiguration ftConfiguration = FallbackTreatmentsConfiguration.builder()
                .byFactory(byFactoryConfig)
                .build();
        SplitClientConfig config = SplitClientConfig.builder()
                .fallbackTreatments(ftConfiguration)
                .build();

        assertSame(ftConfiguration, config.fallbackTreatments());
        assertEquals(byFactoryConfig, config.fallbackTreatments().getByFactory());
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
