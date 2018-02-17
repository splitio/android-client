package io.split.android.client.utils;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientImpl;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;

import static org.mockito.Mockito.mock;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class SplitClientImplFactory {

    public static SplitClientImpl get(Key key, SplitFetcher splitFetcher) {
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                SplitClientConfig.builder().build()
        );
    }

    public static SplitClientImpl get(Key key, SplitFetcher splitFetcher, ImpressionListener impressionListener) {
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitFetcher,
                impressionListener,
                new Metrics.NoopMetrics(),
                SplitClientConfig.builder().build()
        );
    }

}
