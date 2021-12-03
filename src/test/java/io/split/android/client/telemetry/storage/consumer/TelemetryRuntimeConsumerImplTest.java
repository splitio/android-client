package io.split.android.client.telemetry.storage.consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
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

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.streaming.ConnectionEstablishedStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryRuntimeConsumerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryRuntimeConsumer telemetryRuntimeConsumer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryRuntimeConsumer = new TelemetryRuntimeConsumerImpl(telemetryStorage);
    }

    @Test
    public void getImpressionsStatsFetchesValuesFromStorage() {
        when(telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED)).thenReturn(2L);
        when(telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED)).thenReturn(1L);
        when(telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED)).thenReturn(3L);

        long queuedImpressions = telemetryRuntimeConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED);
        long dedupedImpressions = telemetryRuntimeConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED);
        long droppedImpressions = telemetryRuntimeConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED);

        verify(telemetryStorage).getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED);
        verify(telemetryStorage).getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED);
        verify(telemetryStorage).getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED);
        assertEquals(2L, queuedImpressions);
        assertEquals(1L, dedupedImpressions);
        assertEquals(3L, droppedImpressions);
    }

    @Test
    public void getEventsStatsFetchesValuesFromStorage() {
        when(telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED)).thenReturn(4L);
        when(telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED)).thenReturn(2L);

        long eventsDropped = telemetryRuntimeConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED);
        long eventsQueued = telemetryRuntimeConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED);

        verify(telemetryStorage).getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED);
        verify(telemetryStorage).getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED);
        assertEquals(4L, eventsDropped);
        assertEquals(2L, eventsQueued);
    }

    @Test
    public void getLastSynchronizationFetchesValuesFromStorage() {
        LastSync mockLastSync = new LastSync();
        mockLastSync.setLastTokenRefresh(1000);
        when(telemetryStorage.getLastSynchronization()).thenReturn(mockLastSync);

        LastSync lastSynchronization = telemetryRuntimeConsumer.getLastSynchronization();

        verify(telemetryStorage).getLastSynchronization();
        assertEquals(1000, lastSynchronization.getLastTokenRefresh());
    }

    @Test
    public void popHttpErrorsFetchesValuesFromStorage() {
        HttpErrors mockHttpErrors = new HttpErrors();
        Map<Long, Long> errorMap = new HashMap<>();
        errorMap.put(400L, 1L);
        mockHttpErrors.setTokenGetErrs(errorMap);
        when(telemetryStorage.popHttpErrors()).thenReturn(mockHttpErrors);

        HttpErrors httpErrors = telemetryRuntimeConsumer.popHttpErrors();

        verify(telemetryStorage).popHttpErrors();
        assertEquals(errorMap, httpErrors.getTokenGetErrs());
    }

    @Test
    public void popHttpLatenciesFetchesValuesFromStorage() {
        HttpLatencies mockHttpLatencies = new HttpLatencies();
        List<Long> mockBucketArray = Arrays.asList(1L, 4L);
        mockHttpLatencies.setImpressionsCount(mockBucketArray);
        when(telemetryStorage.popHttpLatencies()).thenReturn(mockHttpLatencies);

        HttpLatencies httpLatencies = telemetryRuntimeConsumer.popHttpLatencies();

        verify(telemetryStorage).popHttpLatencies();
        assertEquals(mockBucketArray, httpLatencies.getImpressionsCount());
    }

    @Test
    public void popAuthRejectionsFetchesValuesFromStorage() {
        when(telemetryStorage.popAuthRejections()).thenReturn(1L);

        long authRejections = telemetryRuntimeConsumer.popAuthRejections();

        verify(telemetryStorage).popAuthRejections();
        assertEquals(1L, authRejections);
    }

    @Test
    public void popTokenRefreshesFetchesValuesFromStorage() {
        when(telemetryStorage.popTokenRefreshes()).thenReturn(4L);

        long tokenRefreshes = telemetryRuntimeConsumer.popTokenRefreshes();

        verify(telemetryStorage).popTokenRefreshes();
        assertEquals(4L, tokenRefreshes);
    }

    @Test
    public void popStreamingEventsFetchesValuesFromStorage() {
        List<StreamingEvent> mockStreamingEventList = Collections.singletonList(new ConnectionEstablishedStreamingEvent(1000));
        when(telemetryStorage.popStreamingEvents()).thenReturn(mockStreamingEventList);

        List<StreamingEvent> streamingEvents = telemetryRuntimeConsumer.popStreamingEvents();

        verify(telemetryStorage).popStreamingEvents();
        assertEquals(mockStreamingEventList, streamingEvents);
    }

    @Test
    public void popTagsFetchesValuesFromStorage() {
        List<String> mockTagList = Arrays.asList("tag1", "tag2");
        when(telemetryStorage.popTags()).thenReturn(mockTagList);

        List<String> tags = telemetryRuntimeConsumer.popTags();

        verify(telemetryStorage).popTags();
        assertEquals(mockTagList, tags);
    }

    @Test
    public void getSessionLengthFetchesValuesFromStorage() {
        when(telemetryStorage.getSessionLength()).thenReturn(20L);

        long sessionLength = telemetryRuntimeConsumer.getSessionLength();

        verify(telemetryStorage).getSessionLength();
        assertEquals(20L, sessionLength);
    }
}
