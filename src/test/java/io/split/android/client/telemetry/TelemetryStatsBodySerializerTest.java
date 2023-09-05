package io.split.android.client.telemetry;

import static junit.framework.TestCase.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.model.UpdatesFromSSE;
import io.split.android.client.telemetry.model.streaming.ConnectionEstablishedStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;

public class TelemetryStatsBodySerializerTest {

    private TelemetryStatsBodySerializer telemetryStatsBodySerializer;

    @Before
    public void setUp() {
        telemetryStatsBodySerializer = new TelemetryStatsBodySerializer();
    }

    @Test
    public void jsonIsBuiltAsExpected() {
        String serializedStats = telemetryStatsBodySerializer.serialize(getMockStats());

        assertEquals("{\"lS\":{\"sp\":1000,\"ms\":2000,\"im\":3000,\"ic\":4000,\"ev\":5000,\"te\":6000,\"to\":7000},\"mL\":{\"t\":[0,0,2,0],\"ts\":[0,0,3,0],\"tc\":[0,0,5,0],\"tcs\":[0,0,4,0],\"tf\":[1,0,0,0],\"tfs\":[2,0,0,0],\"tfc\":[3,0,0,0],\"tfcs\":[4,0,0,0],\"tr\":[0,0,1,0]},\"mE\":{\"t\":2,\"ts\":3,\"tc\":5,\"tcs\":4,\"tf\":10,\"tfs\":20,\"tfc\":30,\"tfcs\":40,\"tr\":1},\"hE\":{},\"hL\":{\"sp\":[0,0,3,0],\"ms\":[0,0,5,0],\"im\":[0,0,1,0],\"ic\":[0,0,4,0],\"ev\":[0,0,2,0],\"te\":[1,0,0,0],\"to\":[0,0,6,0]},\"tR\":4,\"aR\":5,\"iQ\":2,\"iDe\":5,\"iDr\":4,\"spC\":456,\"seC\":4,\"skC\":1,\"sL\":2000,\"eQ\":4,\"eD\":2,\"sE\":[{\"e\":0,\"t\":5000},{\"e\":20,\"d\":4,\"t\":2000}],\"t\":[\"tag1\",\"tag2\"],\"ufs\":{\"sp\":4,\"ms\":8}}", serializedStats);
    }

    private Stats getMockStats() {
        Stats stats = new Stats();

        HttpErrors httpErrors = new HttpErrors();
        stats.setHttpErrors(httpErrors);

        HttpLatencies httpLatencies = new HttpLatencies();
        httpLatencies.setTelemetry(Arrays.asList(1L, 0L, 0L, 0L));
        httpLatencies.setImpressions(Arrays.asList(0L, 0L, 1L, 0L));
        httpLatencies.setEvents(Arrays.asList(0L, 0L, 2L, 0L));
        httpLatencies.setSplits(Arrays.asList(0L, 0L, 3L, 0L));
        httpLatencies.setImpressionsCount(Arrays.asList(0L, 0L, 4L, 0L));
        httpLatencies.setMySegments(Arrays.asList(0L, 0L, 5L, 0L));
        httpLatencies.setToken(Arrays.asList(0L, 0L, 6L, 0L));

        MethodLatencies methodLatencies = new MethodLatencies();
        methodLatencies.setTrack(Arrays.asList(0L, 0L, 1L, 0L));
        methodLatencies.setTreatment(Arrays.asList(0L, 0L, 2L, 0L));
        methodLatencies.setTreatments(Arrays.asList(0L, 0L, 3L, 0L));
        methodLatencies.setTreatmentsWithConfig(Arrays.asList(0L, 0L, 4L, 0L));
        methodLatencies.setTreatmentWithConfig(Arrays.asList(0L, 0L, 5L, 0L));
        methodLatencies.setTreatmentsByFlagSet(Arrays.asList(1L, 0L, 0L, 0L));
        methodLatencies.setTreatmentsByFlagSets(Arrays.asList(2L, 0L, 0L, 0L));
        methodLatencies.setTreatmentsWithConfigByFlagSet(Arrays.asList(3L, 0L, 0L, 0L));
        methodLatencies.setTreatmentsWithConfigByFlagSets(Arrays.asList(4L, 0L, 0L, 0L));

        MethodExceptions methodExceptions = new MethodExceptions();
        methodExceptions.setTrack(1);
        methodExceptions.setTreatment(2);
        methodExceptions.setTreatments(3);
        methodExceptions.setTreatmentsWithConfig(4);
        methodExceptions.setTreatmentWithConfig(5);
        methodExceptions.setTreatmentsByFlagSet(10);
        methodExceptions.setTreatmentsByFlagSets(20);
        methodExceptions.setTreatmentsWithConfigByFlagSet(30);
        methodExceptions.setTreatmentsWithConfigByFlagSets(40);

        stats.setHttpLatencies(httpLatencies);
        stats.setAuthRejections(5);
        stats.setEventsDropped(2);
        stats.setEventsQueued(4);
        stats.setImpressionsDeduped(5);
        stats.setImpressionsDropped(4);
        stats.setImpressionsQueued(2);
        LastSync lastSynchronizations = new LastSync();
        lastSynchronizations.setLastSplitSync(1000);
        lastSynchronizations.setLastMySegmentSync(2000);
        lastSynchronizations.setLastImpressionSync(3000);
        lastSynchronizations.setLastImpressionCountSync(4000);
        lastSynchronizations.setLastEventSync(5000);
        lastSynchronizations.setLastTelemetrySync(6000);
        lastSynchronizations.setLastTokenRefresh(7000);
        stats.setLastSynchronizations(lastSynchronizations);
        stats.setSegmentCount(4);
        stats.setMethodExceptions(methodExceptions);
        stats.setMethodLatencies(methodLatencies);
        stats.setSessionLengthMs(2000);
        stats.setSplitCount(456);
        stats.setTags(Arrays.asList("tag1", "tag2"));
        stats.setTokenRefreshes(4);
        stats.setStreamingEvents(Arrays.asList(new ConnectionEstablishedStreamingEvent(5000), new OccupancySecStreamingEvent(4, 2000)));
        stats.setUpdatesFromSSE(new UpdatesFromSSE(4L, 8L));

        return stats;
    }
}
