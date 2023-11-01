package tests.integration.sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SplitResult;
import io.split.android.client.SyncConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class FlagSetsEvaluationTest {

    private final FileHelper fileHelper = new FileHelper();
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void sdkWithSetsCanOnlyEvaluateSetsConfigured() throws IOException, InterruptedException {
        /*
            Initialize with set_1 configured. Changes contains 1 flag in set_1 & set_2, and one in set_3.

            Only the flag in set_1 can be evaluated.
         */
        SplitClient client = getClient(mContext, DatabaseHelper.getTestDatabase(mContext), "set_1");

        Map<String, String> set1Treatments = client.getTreatmentsByFlagSet("set_1", null);
        Map<String, String> set2Treatments = client.getTreatmentsByFlagSet("set_2", null);
        Map<String, String> set3Treatments = client.getTreatmentsByFlagSet("set_3", null);
        Map<String, SplitResult> set1Results = client.getTreatmentsWithConfigByFlagSet("set_1", null);
        Map<String, SplitResult> set2Results = client.getTreatmentsWithConfigByFlagSet("set_2", null);
        Map<String, SplitResult> set3Results = client.getTreatmentsWithConfigByFlagSet("set_3", null);
        Map<String, String> allTreatments = client.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2", "set_3"), null);
        Map<String, SplitResult> allTreatmentsWithConfig = client.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2", "set_3"), null);

        assertEquals(1, set1Treatments.size());
        assertEquals(0, set2Treatments.size());
        assertEquals(0, set3Treatments.size());
        assertEquals(1, set1Results.size());
        assertEquals(0, set2Results.size());
        assertEquals(0, set3Results.size());
        assertEquals(1, allTreatments.size());
        assertEquals(1, allTreatmentsWithConfig.size());
        assertTrue(set1Treatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(set1Results.values().stream().noneMatch(t -> t.treatment().equals("control")));
        assertTrue(allTreatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(allTreatmentsWithConfig.values().stream().noneMatch(t -> t.treatment().equals("control")));
    }

    @Test
    public void sdkWithoutSetsCanEvaluateAnySet() throws IOException, InterruptedException {
        /*
            Initialize with no sets configured. Changes contains 1 flag in set_1 & set_2, and one in set_3.

            All flags can be evaluated by sets.
         */
        SplitClient client = getClient(mContext, DatabaseHelper.getTestDatabase(mContext));

        Map<String, String> set1Treatments = client.getTreatmentsByFlagSet("set_1", null);
        Map<String, String> set2Treatments = client.getTreatmentsByFlagSet("set_2", null);
        Map<String, String> set3Treatments = client.getTreatmentsByFlagSet("set_3", null);
        Map<String, SplitResult> set1Results = client.getTreatmentsWithConfigByFlagSet("set_1", null);
        Map<String, SplitResult> set2Results = client.getTreatmentsWithConfigByFlagSet("set_2", null);
        Map<String, SplitResult> set3Results = client.getTreatmentsWithConfigByFlagSet("set_3", null);
        Map<String, String> allTreatments = client.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2", "set_3"), null);
        Map<String, SplitResult> allTreatmentsWithConfig = client.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2", "set_3"), null);

        assertEquals(1, set1Treatments.size());
        assertEquals(1, set2Treatments.size());
        assertEquals(1, set3Treatments.size());
        assertEquals(1, set1Results.size());
        assertEquals(1, set2Results.size());
        assertEquals(1, set3Results.size());
        assertTrue(set1Treatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(set2Treatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(set3Treatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(set1Results.values().stream().noneMatch(t -> t.treatment().equals("control")));
        assertTrue(set2Results.values().stream().noneMatch(t -> t.treatment().equals("control")));
        assertTrue(set3Results.values().stream().noneMatch(t -> t.treatment().equals("control")));
        assertTrue(allTreatments.values().stream().noneMatch(t -> t.equals("control")));
        assertTrue(allTreatmentsWithConfig.values().stream().noneMatch(t -> t.treatment().equals("control")));
    }

    private SplitClient getClient(
            Context mContext,
            SplitRoomDatabase splitRoomDatabase,
            String... sets) throws IOException, InterruptedException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .trafficType("client")
                .enableDebug()
                .impressionsRefreshRate(1000)
                .impressionsCountersRefreshRate(1000)
                .syncConfig(SyncConfig.builder()
                        .addSplitFilter(SplitFilter.bySet(Arrays.asList(sets)))
                        .build())
                .featuresRefreshRate(2)
                .streamingEnabled(false)
                .eventFlushInterval(1000)
                .build();

        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);

            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChangeWithSet(2));
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses);

        SplitFactory factory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                new HttpClientMock(httpResponseMockDispatcher),
                splitRoomDatabase, null, null, null);

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));

        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);

        if (readyAwait) {
            return client;
        } else {
            return null;
        }
    }

    private String loadSplitChangeWithSet(int setsCount) {
        String change = fileHelper.loadFileContent(mContext, "split_changes_flag_set-" + setsCount + ".json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;

        return Json.toJson(parsedChange);
    }
}
