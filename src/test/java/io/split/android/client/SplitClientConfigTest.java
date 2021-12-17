package io.split.android.client;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class SplitClientConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_feature_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_segment_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .segmentsRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_impression_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_metrics_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .metricsRefreshRate(29)
                .build();
    }

    @Test
    public void can_set_refresh_rates_to__30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .metricsRefreshRate(30)
                .build();
    }

    @Test
    public void telemetry_refresh_rate_less_than_60_sets_value_to_default() {
        SplitClientConfig config = SplitClientConfig.builder()
                .telemetryRefreshRate(30)
                .build();

        assertEquals(3600, config.telemetryRefreshRate());
    }

    @Test
    public void telemetry_refresh_rate_greater_than_60_is_accepted() {
        SplitClientConfig config = SplitClientConfig.builder()
                .telemetryRefreshRate(120)
                .build();

        assertEquals(120, config.telemetryRefreshRate());
    }
}
