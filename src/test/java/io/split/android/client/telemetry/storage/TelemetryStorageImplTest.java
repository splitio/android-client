package io.split.android.client.telemetry.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.AblyErrorStreamingEvent;
import io.split.android.client.telemetry.model.streaming.ConnectionEstablishedStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;

public class TelemetryStorageImplTest {

    @Mock ILatencyTracker latencyTracker;
    private TelemetryStorageImpl telemetryStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryStorage = new TelemetryStorageImpl(latencyTracker);
    }

    @Test
    public void popExceptionsReturnsCorrectlyBuiltMethodExceptions() {
        telemetryStorage.recordException(Method.TRACK);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENTS);
        telemetryStorage.recordException(Method.TREATMENTS_WITH_CONFIG);
        telemetryStorage.recordException(Method.TREATMENT_WITH_CONFIG);

        MethodExceptions methodExceptions = telemetryStorage.popExceptions();

        assertEquals(1, methodExceptions.getTrack());
        assertEquals(2, methodExceptions.getTreatment());
        assertEquals(1, methodExceptions.getTreatments());
        assertEquals(1, methodExceptions.getTreatmentsWithConfig());
        assertEquals(1, methodExceptions.getTreatmentWithConfig());
    }

    @Test
    public void popExceptionsEmptiesCounters() {
        telemetryStorage.recordException(Method.TRACK);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENTS);
        telemetryStorage.recordException(Method.TREATMENTS_WITH_CONFIG);
        telemetryStorage.recordException(Method.TREATMENT_WITH_CONFIG);

        telemetryStorage.popExceptions();

        MethodExceptions secondPop = telemetryStorage.popExceptions();

        assertEquals(0, secondPop.getTrack());
        assertEquals(0, secondPop.getTreatment());
        assertEquals(0, secondPop.getTreatments());
        assertEquals(0, secondPop.getTreatmentsWithConfig());
        assertEquals(0, secondPop.getTreatmentWithConfig());
    }

    @Test
    public void popLatenciesReturnsCorrectlyBuiltObject() {
        telemetryStorage.recordLatency(Method.TRACK, 200);
        telemetryStorage.recordLatency(Method.TREATMENT, 10022);
        telemetryStorage.recordLatency(Method.TREATMENT, 300);
        telemetryStorage.recordLatency(Method.TREATMENTS, 200);
        telemetryStorage.recordLatency(Method.TREATMENTS_WITH_CONFIG, 10);
        telemetryStorage.recordLatency(Method.TREATMENT_WITH_CONFIG, 2000);

        MethodLatencies methodLatencies = telemetryStorage.popLatencies();

        assertFalse(methodLatencies.getTrack().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatment().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatments().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatmentsWithConfig().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatmentWithConfig().stream().allMatch(l -> l == 0));
    }

    @Test
    public void secondLatenciesPopHasArraysSetIn0() {
        telemetryStorage.recordLatency(Method.TRACK, 200);
        telemetryStorage.recordLatency(Method.TREATMENT, 10022);
        telemetryStorage.recordLatency(Method.TREATMENT, 300);
        telemetryStorage.recordLatency(Method.TREATMENTS, 200);
        telemetryStorage.recordLatency(Method.TREATMENTS_WITH_CONFIG, 10);
        telemetryStorage.recordLatency(Method.TREATMENT_WITH_CONFIG, 2000);

        telemetryStorage.popLatencies();

        MethodLatencies methodLatencies = telemetryStorage.popLatencies();

        assertTrue(methodLatencies.getTrack().stream().allMatch(l -> l == 0));
        assertTrue(methodLatencies.getTreatment().stream().allMatch(l -> l == 0));
        assertTrue(methodLatencies.getTreatments().stream().allMatch(l -> l == 0));
        assertTrue(methodLatencies.getTreatmentsWithConfig().stream().allMatch(l -> l == 0));
        assertTrue(methodLatencies.getTreatmentWithConfig().stream().allMatch(l -> l == 0));
    }

    @Test
    public void recordBURTimeouts() {
        long initialTimeouts = telemetryStorage.getBURTimeouts();
        telemetryStorage.recordBURTimeout();
        telemetryStorage.recordBURTimeout();

        long burTimeouts = telemetryStorage.getBURTimeouts();

        telemetryStorage.recordBURTimeout();

        long newTimeouts = telemetryStorage.getBURTimeouts();

        assertEquals(0, initialTimeouts);
        assertEquals(2, burTimeouts);
        assertEquals(3, newTimeouts);
    }

    @Test
    public void recordNonReadyUsages() {
        long initialUsages = telemetryStorage.getNonReadyUsage();
        telemetryStorage.recordNonReadyUsage();
        telemetryStorage.recordNonReadyUsage();

        long nonReadyUsages = telemetryStorage.getNonReadyUsage();

        telemetryStorage.recordNonReadyUsage();

        long newNonReadyUsages = telemetryStorage.getNonReadyUsage();

        assertEquals(0, initialUsages);
        assertEquals(2, nonReadyUsages);
        assertEquals(3, newNonReadyUsages);
    }

    @Test
    public void impressionsDataIsStoredCorrectly() {
        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 10);
        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 5);
        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DROPPED, 2);

        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 10);
        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 5);
        telemetryStorage.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DROPPED, 2);

        assertEquals(20, telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED));
    }

    @Test
    public void eventsDataRecordsIsStoredCorrectly() {
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 4);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 5);

        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 2);
        telemetryStorage.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 3);

        assertEquals(6, telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED));
        assertEquals(8, telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED));
    }

    @Test
    public void methodExceptionsCounterIsInitialized() {
        MethodExceptions methodExceptions = telemetryStorage.popExceptions();

        assertNotNull(methodExceptions);
    }

    @Test
    public void httpLatenciesCounterIsInitialized() {
        MethodLatencies methodLatencies = telemetryStorage.popLatencies();

        assertNotNull(methodLatencies);
    }

    @Test
    public void factoryCounterIsInitialized() {
        long burTimeouts = telemetryStorage.getBURTimeouts();
        long nonReadyUsages = telemetryStorage.getNonReadyUsage();

        assertEquals(0, burTimeouts);
        assertEquals(0, nonReadyUsages);
    }

    @Test
    public void impressionsDataIsInitialized() {
        long impressionsStatsDeduped = telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED);
        long impressionsStatsQueued = telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED);
        long impressionsStatsDropped = telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED);

        assertEquals(0, impressionsStatsDeduped);
        assertEquals(0, impressionsStatsQueued);
        assertEquals(0, impressionsStatsDropped);
    }

    @Test
    public void eventsDataIsInitialized() {
        long eventsDropped = telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED);
        long eventsQueued = telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED);

        assertEquals(0, eventsDropped);
        assertEquals(0, eventsQueued);
    }

    @Test
    public void lastSyncDataIsInitialized() {
        LastSync lastSynchronization = telemetryStorage.getLastSynchronization();

        assertNotNull(lastSynchronization);
    }

    @Test
    public void httpErrorsIsInitialized() {
        HttpErrors httpErrors = telemetryStorage.popHttpErrors();

        assertNotNull(httpErrors);
    }

    @Test
    public void httpLatenciesIsInitialized() {
        HttpLatencies httpLatencies = telemetryStorage.popHttpLatencies();

        assertNotNull(httpLatencies);
    }

    @Test
    public void pushCountersIsInitialized() {
        telemetryStorage.popAuthRejections();
        telemetryStorage.popTokenRefreshes();
    }

    @Test
    public void lastSyncDataBuildsCorrectly() {
        telemetryStorage.recordSuccessfulSync(OperationType.EVENTS, 1000);
        telemetryStorage.recordSuccessfulSync(OperationType.TELEMETRY, 2000);
        telemetryStorage.recordSuccessfulSync(OperationType.IMPRESSIONS, 3000);
        telemetryStorage.recordSuccessfulSync(OperationType.IMPRESSIONS_COUNT, 4000);
        telemetryStorage.recordSuccessfulSync(OperationType.MY_SEGMENT, 5000);
        telemetryStorage.recordSuccessfulSync(OperationType.SPLITS, 6000);
        telemetryStorage.recordSuccessfulSync(OperationType.TOKEN, 7000);

        LastSync lastSync = telemetryStorage.getLastSynchronization();

        assertEquals(1000, lastSync.getLastEventSync());
        assertEquals(2000, lastSync.getLastTelemetrySync());
        assertEquals(3000, lastSync.getLastImpressionSync());
        assertEquals(4000, lastSync.getLasImpressionCountSync());
        assertEquals(5000, lastSync.getLastSegmentSync());
        assertEquals(6000, lastSync.getLastSplitSync());
        assertEquals(7000, lastSync.getLastTokenRefresh());
    }

    @Test
    public void sessionDataStorage() {
        telemetryStorage.recordSessionLength(250);

        long sessionLength = telemetryStorage.getSessionLength();

        assertEquals(250, sessionLength);
    }

    @Test
    public void popHttpErrorsBuildObjectCorrectly() {
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS_COUNT, 400);
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS_COUNT, 400);
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS_COUNT, 404);
        telemetryStorage.recordSyncError(OperationType.EVENTS, 401);
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS, 401);
        telemetryStorage.recordSyncError(OperationType.TELEMETRY, 401);
        telemetryStorage.recordSyncError(OperationType.MY_SEGMENT, 401);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 401);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 404);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 404);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 404);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 500);
        telemetryStorage.recordSyncError(OperationType.TOKEN, 401);

        HttpErrors httpErrors = telemetryStorage.popHttpErrors();

        Map<Long, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put(400L, 2L);
        expectedCountMap.put(404L, 1L);

        Map<Long, Long> expectedSplitSyncMap = new HashMap<>();
        expectedSplitSyncMap.put(500L, 1L);
        expectedSplitSyncMap.put(404L, 3L);
        expectedSplitSyncMap.put(401L, 1L);

        Map<Long, Long> expectedEventMap = new HashMap<>();
        expectedEventMap.put(401L, 1L);

        assertEquals(expectedCountMap, httpErrors.getImpressionCountSyncErrs());
        assertEquals(expectedEventMap, httpErrors.getEventsSyncErrs());
        assertEquals(expectedEventMap, httpErrors.getImpressionSyncErrs());
        assertEquals(expectedEventMap, httpErrors.getTelemetrySyncErrs());
        assertEquals(expectedEventMap, httpErrors.getSegmentSyncErrs());
        assertEquals(expectedSplitSyncMap, httpErrors.getSplitSyncErrs());
        assertEquals(expectedEventMap, httpErrors.getTokenGetErrs());
    }

    @Test
    public void popHttpErrorsReinitializesMap() {
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS_COUNT, 400);
        telemetryStorage.recordSyncError(OperationType.EVENTS, 401);
        telemetryStorage.recordSyncError(OperationType.IMPRESSIONS, 401);
        telemetryStorage.recordSyncError(OperationType.TELEMETRY, 401);
        telemetryStorage.recordSyncError(OperationType.MY_SEGMENT, 401);
        telemetryStorage.recordSyncError(OperationType.SPLITS, 401);
        telemetryStorage.recordSyncError(OperationType.TOKEN, 401);

        telemetryStorage.popHttpErrors();
        HttpErrors httpErrors = telemetryStorage.popHttpErrors();

        assertTrue(httpErrors.getImpressionCountSyncErrs().isEmpty());
        assertTrue(httpErrors.getEventsSyncErrs().isEmpty());
        assertTrue(httpErrors.getImpressionSyncErrs().isEmpty());
        assertTrue(httpErrors.getTelemetrySyncErrs().isEmpty());
        assertTrue(httpErrors.getSegmentSyncErrs().isEmpty());
        assertTrue(httpErrors.getSplitSyncErrs().isEmpty());
        assertTrue(httpErrors.getTokenGetErrs().isEmpty());
    }

    @Test
    public void popHttpLatenciesBuildsObjectCorrectly() {
        telemetryStorage.recordSyncLatency(OperationType.TELEMETRY, 200);
        telemetryStorage.recordSyncLatency(OperationType.SPLITS, 10022);
        telemetryStorage.recordSyncLatency(OperationType.EVENTS, 300);
        telemetryStorage.recordSyncLatency(OperationType.IMPRESSIONS, 200);
        telemetryStorage.recordSyncLatency(OperationType.IMPRESSIONS_COUNT, 10);
        telemetryStorage.recordSyncLatency(OperationType.MY_SEGMENT, 2000);
        telemetryStorage.recordSyncLatency(OperationType.TOKEN, 2000);

        HttpLatencies httpLatencies = telemetryStorage.popHttpLatencies();

        assertFalse(httpLatencies.getTelemetry().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getEvents().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getImpressions().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getImpressionsCount().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getSplits().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getToken().stream().allMatch(l -> l == 0));
        assertFalse(httpLatencies.getSegments().stream().allMatch(l -> l == 0));
    }

    @Test
    public void popHttpLatenciesClearsArray() {
        telemetryStorage.recordSyncLatency(OperationType.TELEMETRY, 200);
        telemetryStorage.recordSyncLatency(OperationType.SPLITS, 10022);
        telemetryStorage.recordSyncLatency(OperationType.EVENTS, 300);
        telemetryStorage.recordSyncLatency(OperationType.IMPRESSIONS, 200);
        telemetryStorage.recordSyncLatency(OperationType.IMPRESSIONS_COUNT, 10);
        telemetryStorage.recordSyncLatency(OperationType.MY_SEGMENT, 2000);
        telemetryStorage.recordSyncLatency(OperationType.TOKEN, 2000);

        telemetryStorage.popHttpLatencies();
        HttpLatencies httpLatencies = telemetryStorage.popHttpLatencies();

        assertTrue(httpLatencies.getTelemetry().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getEvents().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getImpressions().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getImpressionsCount().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getSplits().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getToken().stream().allMatch(l -> l == 0));
        assertTrue(httpLatencies.getSegments().stream().allMatch(l -> l == 0));
    }

    @Test
    public void authRejectionsAreProperlyCounted() {

        assertEquals(0, telemetryStorage.popAuthRejections());

        telemetryStorage.recordAuthRejections();
        telemetryStorage.recordAuthRejections();
        telemetryStorage.recordAuthRejections();

        assertEquals(3, telemetryStorage.popAuthRejections());

        assertEquals(0, telemetryStorage.popAuthRejections());
    }

    @Test
    public void tokenRefreshesAreProperlyCounted() {

        assertEquals(0, telemetryStorage.popTokenRefreshes());

        telemetryStorage.recordTokenRefreshes();
        telemetryStorage.recordTokenRefreshes();

        assertEquals(2, telemetryStorage.popTokenRefreshes());

        assertEquals(0, telemetryStorage.popTokenRefreshes());
    }

    @Test
    public void tagsAreStoredCorrectly() {
        assertTrue(telemetryStorage.popTags().isEmpty());
        telemetryStorage.addTag("tag1");
        telemetryStorage.addTag("tag2");

        List<String> tags = telemetryStorage.popTags();
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
    }

    @Test
    public void popTagsResetsTagsSet() {
        assertTrue(telemetryStorage.popTags().isEmpty());
        telemetryStorage.addTag("tag1");
        telemetryStorage.addTag("tag2");

        telemetryStorage.popTags();

        assertTrue(telemetryStorage.popTags().isEmpty());
    }

    @Test
    public void addingSameTagMultipleTimesReturnsOnlyOneInstance() {
        assertTrue(telemetryStorage.popTags().isEmpty());
        telemetryStorage.addTag("tag1");
        telemetryStorage.addTag("tag1");
        telemetryStorage.addTag("tag1");
        telemetryStorage.addTag("tag2");

        List<String> tags = telemetryStorage.popTags();

        assertEquals(2, tags.size());
    }

    @Test
    public void onlyStoreUpTo10Tags() {
        assertTrue(telemetryStorage.popTags().isEmpty());
        for (int i = 0; i < 12; i++) {
            telemetryStorage.addTag("tag" + i);
        }

        List<String> tags = telemetryStorage.popTags();

        assertEquals(10, tags.size());
    }

    @Test
    public void streamingEventsAreAddedCorrectly() {
        telemetryStorage.recordStreamingEvents(new ConnectionEstablishedStreamingEvent(1000));
        telemetryStorage.recordStreamingEvents(new AblyErrorStreamingEvent(200, 1000));
        telemetryStorage.recordStreamingEvents(new StreamingStatusStreamingEvent(StreamingStatusStreamingEvent.Status.ENABLED, 1000));
        telemetryStorage.recordStreamingEvents(new OccupancySecStreamingEvent(10, 1000));
        telemetryStorage.recordStreamingEvents(new TokenRefreshStreamingEvent(2000, 1000));
        telemetryStorage.recordStreamingEvents(new TokenRefreshStreamingEvent(3000, 2000));

        List<StreamingEvent> streamingEvents = telemetryStorage.popStreamingEvents();

        assertEquals(6, streamingEvents.size());
    }

    @Test
    public void popStreamingEventClearsStoredList() {
        telemetryStorage.recordStreamingEvents(new ConnectionEstablishedStreamingEvent(1000));
        telemetryStorage.recordStreamingEvents(new AblyErrorStreamingEvent(200, 1000));

        telemetryStorage.popStreamingEvents();
        List<StreamingEvent> streamingEvents = telemetryStorage.popStreamingEvents();

        assertTrue(streamingEvents.isEmpty());
    }

    @Test
    public void onlyStoreUpTo20StreamingEventsAtATime() {
        for (int i = 0; i < 25; i++) {
            telemetryStorage.recordStreamingEvents(new ConnectionEstablishedStreamingEvent(1000));
        }
        List<StreamingEvent> streamingEvents = telemetryStorage.popStreamingEvents();

        assertEquals(20, streamingEvents.size());
    }
}
