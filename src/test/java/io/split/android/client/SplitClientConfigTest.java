package io.split.android.client;

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
}
