package io.split.android.client.utils;

import java.util.ArrayList;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientImpl;
import io.split.android.client.SplitFactory;
import io.split.android.client.TrackClient;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;
import io.split.android.fake.SplitCacheStub;

import static org.mockito.Mockito.mock;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class SplitClientImplFactory {

    public static SplitClientImpl get(Key key, SplitFetcher splitFetcher) {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventsManager = new SplitEventsManager(cfg);


        SplitClientImpl c = new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                cfg,
                eventsManager,
                mock(TrackClient.class),
                new SplitCacheStub(new ArrayList<>())
        );
        eventsManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);
        eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
        return c;
    }

    public static SplitClientImpl get(Key key, SplitFetcher splitFetcher, ImpressionListener impressionListener) {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitFetcher,
                impressionListener,
                new Metrics.NoopMetrics(),
                cfg,
                new SplitEventsManager(cfg),
                mock(TrackClient.class),
                new SplitCacheStub(new ArrayList<>())
        );
    }

    public static SplitClientImpl get(Key key, SplitFetcher splitFetcher, SplitEventsManager eventsManager) {
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitFetcher,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                SplitClientConfig.builder().build(),
                eventsManager,
                mock(TrackClient.class),
                new SplitCacheStub(new ArrayList<>())
        );
    }


}
