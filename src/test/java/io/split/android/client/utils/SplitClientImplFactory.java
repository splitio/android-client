package io.split.android.client.utils;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientImpl;
import io.split.android.client.SplitFactory;
import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.NewSyncManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.metrics.Metrics;

import static org.mockito.Mockito.mock;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class SplitClientImplFactory {

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage) {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventsManager = new SplitEventsManager(cfg);
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorage.class));

        SplitClientImpl c = new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                cfg,
                eventsManager,
                mock(SplitsStorage.class),
                mock(EventPropertiesProcessor.class),
                mock(NewSyncManager.class)
        );
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            eventsManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);
        eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
        return c;
    }

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage, ImpressionListener impressionListener) {
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorage.class));
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitParser,
                impressionListener,
                new Metrics.NoopMetrics(),
                cfg,
                new SplitEventsManager(cfg),
                mock(SplitsStorage.class),
                mock(EventPropertiesProcessor.class),
                mock(NewSyncManager.class)
        );
    }

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage, SplitEventsManager eventsManager) {
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorage.class));
        return new SplitClientImpl(
                mock(SplitFactory.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                new Metrics.NoopMetrics(),
                SplitClientConfig.builder().build(),
                eventsManager,
                mock(SplitsStorage.class),
                mock(EventPropertiesProcessor.class),
                mock(NewSyncManager.class)
        );
    }
}
