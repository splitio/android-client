package io.split.android.client.utils;

import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;

import java.util.Collections;

import io.split.android.client.EventsTracker;
import io.split.android.client.FlagSetsFilterImpl;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientImpl;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.DecoratedImpressionListener;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerFactory;
import io.split.android.client.validators.TreatmentManagerFactoryImpl;
import io.split.android.engine.experiments.ParserCommons;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.fake.SplitTaskExecutorStub;

@Deprecated
public class SplitClientImplFactory {

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage) {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventsManager = new SplitEventsManager(cfg, new SplitTaskExecutorStub());
        SplitParser splitParser = getSplitParser();
        TelemetryStorage telemetryStorage = mock(TelemetryStorage.class);
        TreatmentManagerFactory treatmentManagerFactory = new TreatmentManagerFactoryImpl(
                new KeyValidatorImpl(), new SplitValidatorImpl(), new ImpressionListener.FederatedImpressionListener(mock(DecoratedImpressionListener.class), Collections.emptyList()),
                false, new AttributesMergerImpl(), telemetryStorage, splitParser,
                new FlagSetsFilterImpl(Collections.emptySet()), splitsStorage);

        AttributesManager attributesManager = mock(AttributesManager.class);
        SplitClientImpl c = new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                cfg,
                eventsManager,
                mock(EventsTracker.class),
                attributesManager,
                mock(SplitValidator.class),
                treatmentManagerFactory.getTreatmentManager(key, eventsManager, attributesManager)
        );
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
        eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        return c;
    }

    public static SplitClientImpl get(Key key, ImpressionListener impressionListener) {
        SplitParser splitParser = getSplitParser();
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        return new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                impressionListener,
                cfg,
                new SplitEventsManager(cfg, new SplitTaskExecutorStub()),
                mock(EventsTracker.class),
                mock(AttributesManager.class),
                mock(SplitValidator.class),
                mock(TreatmentManager.class)
        );
    }

    public static SplitClientImpl get(Key key, SplitEventsManager eventsManager) {
        SplitParser splitParser = getSplitParser();
        return new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                SplitClientConfig.builder().build(),
                eventsManager,
                mock(EventsTracker.class),
                mock(AttributesManager.class),
                mock(SplitValidator.class),
                mock(TreatmentManager.class)
        );
    }

    @NonNull
    private static SplitParser getSplitParser() {
        return new SplitParser(mock(ParserCommons.class));
    }
}
