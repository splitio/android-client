package io.split.android.client.storage.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.MockedStatic;

import io.split.android.client.service.impressions.observer.PersistentImpressionsObserverCacheStorage;
import io.split.android.client.storage.attributes.AttributesStorageContainer;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.PersistentRuleBasedSegmentStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.engine.experiments.ParserCommons;

public class SplitStorageContainerTest {

    @Test
    public void ruleBasedSegmentStorageIsSetInParserCommons() {
        try (MockedStatic<RuleBasedSegmentStorageInitializer> mockStatic = mockStatic(RuleBasedSegmentStorageInitializer.class)) {
            RuleBasedSegmentStorageInitializer.Result result = mock(RuleBasedSegmentStorageInitializer.Result.class);
            MySegmentsStorageContainer segmentStorage = mock(MySegmentsStorageContainer.class);
            MySegmentsStorageContainer largeSegmentStorage = mock(MySegmentsStorageContainer.class);
            PersistentRuleBasedSegmentStorage persistentRbsStorage = mock(PersistentRuleBasedSegmentStorage.class);
            mockStatic.when(() -> RuleBasedSegmentStorageInitializer.initialize(segmentStorage, largeSegmentStorage, persistentRbsStorage)).thenReturn(result);
            SplitStorageContainer container = new SplitStorageContainer(
                    mock(SplitsStorage.class),
                    segmentStorage,
                    largeSegmentStorage,
                    mock(PersistentSplitsStorage.class),
                    mock(EventsStorage.class),
                    mock(PersistentEventsStorage.class),
                    mock(ImpressionsStorage.class),
                    mock(PersistentImpressionsStorage.class),
                    mock(PersistentImpressionsCountStorage.class),
                    mock(PersistentImpressionsUniqueStorage.class),
                    mock(AttributesStorageContainer.class),
                    mock(PersistentAttributesStorage.class),
                    mock(TelemetryStorage.class),
                    mock(PersistentImpressionsObserverCacheStorage.class),
                    mock(GeneralInfoStorage.class),
                    persistentRbsStorage);

            verify(RuleBasedSegmentStorageInitializer.initialize(segmentStorage, largeSegmentStorage, persistentRbsStorage));
        }
    }

    @Test
    public void RuleBasedSegmentStorageInitializerSetsStorageInParserCommons() {
        ParserCommons parserCommons = mock(ParserCommons.class);
        RuleBasedSegmentStorage ruleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
        RuleBasedSegmentStorageInitializer.initialize(parserCommons, ruleBasedSegmentStorage);

        verify(parserCommons).setRuleBasedSegmentStorage(ruleBasedSegmentStorage);
    }
}
