package io.split.android.client.telemetry.storage;

import static org.junit.Assert.*;

import com.google.common.util.concurrent.Runnables;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.synchronizer.ThreadUtils;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.LastSynchronizationRecords;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;

public class TelemetryStorageImplTest {

    private TelemetryStorageImpl telemetryStorage;

    @Before
    public void setUp() {
        telemetryStorage = new TelemetryStorageImpl();
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

    /*
    initializeMethodExceptionsCounter();
        initializeHttpLatenciesCounter();
        initializeFactoryCounters();
        initializeImpressionsData();
        initializeEventsData();
        initializeLastSynchronizationData();

     */
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
    public void lastSyncDataBuildsCorrectly() {
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.EVENTS, 1000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.TELEMETRY, 2000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.IMPRESSIONS, 3000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.IMPRESSIONS_COUNT, 4000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.MY_SEGMENT, 5000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.SPLITS, 6000);
        telemetryStorage.recordSuccessfulSync(LastSynchronizationRecords.TOKEN, 7000);

        LastSync lastSync = telemetryStorage.getLastSynchronization();

        assertEquals(1000, lastSync.getLastEventSync());
        assertEquals(2000, lastSync.getLastTelemetrySync());
        assertEquals(3000, lastSync.getLastImpressionSync());
        assertEquals(4000, lastSync.getLasImpressionCountSync());
        assertEquals(5000, lastSync.getLastSegmentSync());
        assertEquals(6000, lastSync.getLastSplitSync());
        assertEquals(7000, lastSync.getLastTokenRefresh());
    }
}
