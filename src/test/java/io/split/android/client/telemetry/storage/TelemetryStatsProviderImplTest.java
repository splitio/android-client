package io.split.android.client.telemetry.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Stats;

public class TelemetryStatsProviderImplTest {

    @Mock
    private TelemetryStorageConsumer telemetryStorageConsumer;
    @Mock
    private SplitsStorage splitsStorage;
    @Mock
    private MySegmentsStorageContainer mySegmentsStorageContainer;
    private TelemetryStatsProvider telemetryStatsProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryStatsProvider = new TelemetryStatsProviderImpl(telemetryStorageConsumer, splitsStorage, mySegmentsStorageContainer);
    }

    @Test
    public void clearRemovesExistingStatsFromProvider() {

        when(telemetryStorageConsumer.popTags()).thenReturn(Arrays.asList("tag1", "tag2"));

        Stats initialStats = telemetryStatsProvider.getTelemetryStats();
        assertEquals(Arrays.asList("tag1", "tag2"), initialStats.getTags());

        when(telemetryStorageConsumer.popTags()).thenReturn(Collections.emptyList());
        telemetryStatsProvider.clearStats();
        assertEquals(Collections.emptyList(), telemetryStatsProvider.getTelemetryStats().getTags());
    }
}
