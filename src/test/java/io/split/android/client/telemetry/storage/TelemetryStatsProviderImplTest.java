package io.split.android.client.telemetry.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.model.UpdatesFromSSE;
import io.split.android.client.telemetry.model.streaming.OccupancyPriStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;

public class TelemetryStatsProviderImplTest {

    @Mock
    private TelemetryStorageConsumer telemetryStorageConsumer;
    @Mock
    private SplitsStorage splitsStorage;
    @Mock
    private MySegmentsStorageContainer mySegmentsStorageContainer;
    @Mock
    private MySegmentsStorageContainer myLargeSegmentsStorageContainer;
    private TelemetryStatsProvider telemetryStatsProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryStatsProvider = new TelemetryStatsProviderImpl(telemetryStorageConsumer, splitsStorage, mySegmentsStorageContainer, myLargeSegmentsStorageContainer);
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

    @Test
    public void statsAreCorrectlyBuilt() {

        long mySegmentsUniqueCount = 3;
        long myLargeSegmentsUniqueCount = 152516;
        int splitsCount = 40;

        List<StreamingEvent> streamingEvents = Arrays.asList(
                new OccupancyPriStreamingEvent(2, 23232323L),
                new StreamingStatusStreamingEvent(StreamingStatusStreamingEvent.Status.ENABLED, 23232323L));
        List<String> tags = Arrays.asList("asd1", "asd2");
        MethodLatencies methodLatencies = new MethodLatencies();
        long sessionLength = 25250L;
        LastSync lastSync = new LastSync();
        lastSync.setLastEventSync(4242L);
        lastSync.setLastImpressionSync(22L);
        lastSync.setLastSplitSync(9090L);
        lastSync.setLastMySegmentSync(27182L);
        long impQueued = 212L;
        long impDropped = 22L;
        long impDeduped = 66L;
        MethodExceptions methodExceptions = new MethodExceptions();
        HttpLatencies httpLatencies = new HttpLatencies();
        HttpErrors httpErrors = new HttpErrors();
        long authTokenRefreshes = 2L;
        long authRejections = 24L;
        long eventsQueued = 12L;
        long eventsDropped = 21L;
        long sseSplits = 2L;
        long sseMySegments = 4L;
        long sseMyLargeSegments = 5L;

        Map<String, Split> splits = new HashMap<>();
        for (int i = 0; i < splitsCount; i++) {
            splits.put("split" + i, new Split());
        }

        when(splitsStorage.getAll()).thenReturn(splits);
        when(mySegmentsStorageContainer.getUniqueAmount()).thenReturn(mySegmentsUniqueCount);
        when(myLargeSegmentsStorageContainer.getUniqueAmount()).thenReturn(myLargeSegmentsUniqueCount);
        when(telemetryStorageConsumer.popStreamingEvents()).thenReturn(streamingEvents);
        when(telemetryStorageConsumer.popTags()).thenReturn(tags);
        when(telemetryStorageConsumer.popLatencies()).thenReturn(methodLatencies);
        when(telemetryStorageConsumer.getSessionLength()).thenReturn(sessionLength);
        when(telemetryStorageConsumer.getLastSynchronization()).thenReturn(lastSync);
        when(telemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED)).thenReturn(impQueued);
        when(telemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED)).thenReturn(impDropped);
        when(telemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED)).thenReturn(impDeduped);
        when(telemetryStorageConsumer.popExceptions()).thenReturn(methodExceptions);
        when(telemetryStorageConsumer.popHttpLatencies()).thenReturn(httpLatencies);
        when(telemetryStorageConsumer.popHttpErrors()).thenReturn(httpErrors);
        when(telemetryStorageConsumer.popTokenRefreshes()).thenReturn(authTokenRefreshes);
        when(telemetryStorageConsumer.popAuthRejections()).thenReturn(authRejections);
        when(telemetryStorageConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED)).thenReturn(eventsQueued);
        when(telemetryStorageConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED)).thenReturn(eventsDropped);
        when(telemetryStorageConsumer.popUpdatesFromSSE()).thenReturn(new UpdatesFromSSE(sseSplits, sseMySegments, sseMyLargeSegments));

        Stats stats = telemetryStatsProvider.getTelemetryStats();
        assertEquals(streamingEvents, stats.getStreamingEvents());
        assertEquals(splitsCount, stats.getSplitCount());
        assertEquals(tags, stats.getTags());
        assertEquals(methodLatencies, stats.getMethodLatencies());
        assertEquals(mySegmentsUniqueCount, stats.getSegmentCount());
        assertEquals(myLargeSegmentsUniqueCount, stats.getLargeSegmentCount());
        assertEquals(sessionLength, stats.getSessionLengthMs());
        assertEquals(lastSync, stats.getLastSynchronizations());
        assertEquals(impDropped, stats.getImpressionsDropped());
        assertEquals(impQueued, stats.getImpressionsQueued());
        assertEquals(impDeduped, stats.getImpressionsDeduped());
        assertEquals(methodExceptions, stats.getMethodExceptions());
        assertEquals(httpLatencies, stats.getHttpLatencies());
        assertEquals(httpErrors, stats.getHttpErrors());
        assertEquals(authTokenRefreshes, stats.getTokenRefreshes());
        assertEquals(authRejections, stats.getAuthRejections());
        assertEquals(eventsQueued, stats.getEventsQueued());
        assertEquals(eventsDropped, stats.getEventsDropped());
        assertEquals(sseSplits, stats.getUpdatesFromSSE().getSplits());
        assertEquals(sseMySegments, stats.getUpdatesFromSSE().getMySegments());
    }
}
