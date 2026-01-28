package tests.integration.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.rbsChange;
import static helper.IntegrationHelper.splitChangeV2;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.utils.logger.Logger;
import tests.integration.shared.TestingHelper;

public class RuleBasedSegmentsIntegrationTest {

    private static final String rbsChange0 = getBasicChange("2", "1");

    private static final String rbsChangeGZip = IntegrationHelper.rbsChangeGZip("2", "1", "H4sIAAAAAAAA/0zOwWrDMBAE0H+Zs75At9CGUhpySSiUYoIij1MTSwraFdQY/XtRU5ccd3jDzoLoAmGRz3JSisJA1GkRWGyejq/vWxhodsMw+uN84/7OizDDgN9+Kj172AVXzgL72RkIL4FRf69q4FPsRx1TbMGC4NR/Mb/kVG6t51M4j5G5Pdw/w6zgrq+cD5zoNeWGH5asK+p/4y/d7Hant+3HAQaRF6eEHdwkrF2tXf0JAAD//9JucZnyAAAA");
    private static final String rbsChangeZLib = IntegrationHelper.rbsChangeZlib("2", "1", "eJxMzsFqwzAQBNB/mbO+QLfQhlIackkolGKCIo9TE0sK2hXUGP17UVOXHHd4w86C6AJhkc9yUorCQNRpEVhsno6v71sYaHbDMPrjfOP+zosww4Dffio9e9gFV84C+9kZCC+BUX+vauBT7EcdU2zBguDUfzG/5FRuredTOI+RuT3cP8Os4K6vnA+c6DXlhh+WrCvqf+Mv3ex2p7ftxwEGkRenhB3cJKxdrV39CQAA//8FrVMM");

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private SplitRoomDatabase mRoomDb;
    private final AtomicInteger mSplitChangesHits = new AtomicInteger(0);
    private AtomicReference<String> mCustomSplitChangesResponse;

    @Before
    public void setUp() throws Exception {
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mSplitChangesHits.set(0);
        mCustomSplitChangesResponse = new AtomicReference<>(null);
        mRoomDb.clearAllTables();
    }

    @Test
    public void instantUpdateNotification() throws IOException, InterruptedException {
        successfulInstantUpdateTest(rbsChange0, "\"name\":\"rbs_test\"");
    }

    @Test
    public void instantUpdateNotificationGZip() throws IOException, InterruptedException {
        successfulInstantUpdateTest(rbsChangeGZip, "\"name\":\"rbs_test\"");
    }

    @Test
    public void instantUpdateNotificationZLib() throws IOException, InterruptedException {
        successfulInstantUpdateTest(rbsChangeZLib, "\"name\":\"rbs_test\"");
    }

    @Test
    public void referencedRuleBasedSegmentNotPresentTriggersFetch() throws IOException, InterruptedException {
        // {"name":"rbs_test","status":"ACTIVE","trafficTypeName":"user","excluded":{"keys":[],"segments":[]},"conditions":[{"conditionType":"ROLLOUT","matcherGroup":{"combiner":"AND","matchers":[{"keySelector":{"trafficType":"user"},"matcherType":"IN_RULE_BASED_SEGMENT","negate":false,"userDefinedSegmentMatcherData":{"segmentName":"new_rbs_test"}}]}}]}
        String data = "eyJuYW1lIjoicmJzX3Rlc3QiLCJzdGF0dXMiOiJBQ1RJVkUiLCJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiZXhjbHVkZWQiOnsia2V5cyI6W10sInNlZ21lbnRzIjpbXX0sImNvbmRpdGlvbnMiOlt7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoidXNlciJ9LCJtYXRjaGVyVHlwZSI6IklOX1JVTEVfQkFTRURfU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibmV3X3Jic190ZXN0In19XX19XX0=";
        mCustomSplitChangesResponse.set(IntegrationHelper.splitChangeWithReferencedRbs(3, 3));
        referencedRbsTest(IntegrationHelper.rbsChange("4", "4", data));
    }

    @Test
    public void referencedRuleBasedSegmentInFlagNotPresentTriggersFetch() throws IOException, InterruptedException {
        // {"trafficTypeName":"client","name":"workm","trafficAllocation":100,"trafficAllocationSeed":147392224,"seed":524417105,"status":"ACTIVE","killed":false,"defaultTreatment":"on","changeNumber":4,"algo":2,"configurations":{},"sets":["set_3"],"conditions":[{"conditionType":"ROLLOUT","matcherGroup":{"combiner":"AND","matchers":[{"keySelector":{"trafficType":"user"},"matcherType":"IN_RULE_BASED_SEGMENT","negate":false,"userDefinedSegmentMatcherData":{"segmentName":"new_rbs_test"}}]},"partitions":[{"treatment":"on","size":100},{"treatment":"off","size":0}],"label":"in rule based segment new_rbs_test"}]}
        String data = "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjQsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6WyJzZXRfMyJdLCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJJTl9SVUxFX0JBU0VEX1NFR01FTlQiLCJuZWdhdGUiOmZhbHNlLCJ1c2VyRGVmaW5lZFNlZ21lbnRNYXRjaGVyRGF0YSI6eyJzZWdtZW50TmFtZSI6Im5ld19yYnNfdGVzdCJ9fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjowfV0sImxhYmVsIjoiaW4gcnVsZSBiYXNlZCBzZWdtZW50IG5ld19yYnNfdGVzdCJ9XX0=";
        String change = splitChangeV2("2", "1", "0", data);
        referencedRbsTest(change);
    }

    @Test
    public void evaluation() throws IOException, InterruptedException {
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        mCustomSplitChangesResponse.set(IntegrationHelper.loadSplitChanges(mContext, "split_changes_rbs.json"));
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, IntegrationHelper.dummyUserKey());
        if (readyClient == null) {
            fail("Client not ready");
        }

        assertEquals("on", readyClient.getTreatment("rbs_split", Map.of("email", "test@split.io")));
    }

    @Test
    public void evaluationWithExcludedSegment() throws IOException, InterruptedException {
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        MySegmentEntity mySegment = new MySegmentEntity();
        mySegment.setSegmentList("segment_test");
        mySegment.setUserKey(IntegrationHelper.dummyUserKey().matchingKey());
        mRoomDb.mySegmentDao().update(mySegment);
        mCustomSplitChangesResponse.set(IntegrationHelper.loadSplitChanges(mContext, "split_changes_rbs.json"));
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, IntegrationHelper.dummyUserKey());
        if (readyClient == null) {
            fail("Client not ready");
        }

        assertEquals("off", readyClient.getTreatment("rbs_split", Map.of("email", "test@split.io")));
    }

    @Test
    public void evaluationWithExcludedKey() throws IOException, InterruptedException {
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        String matchingKey = "key22";
        mCustomSplitChangesResponse.set(IntegrationHelper.loadSplitChanges(mContext, "split_changes_rbs.json"));
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, new Key(matchingKey));
        if (readyClient == null) {
            fail("Client not ready");
        }

        assertEquals("off", readyClient.getTreatment("rbs_split", Map.of("email", "test@split.io")));
    }

    private void referencedRbsTest(String notification) throws IOException, InterruptedException {
        // Initialize a factory with RBS enabled
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, IntegrationHelper.dummyUserKey());
        if (readyClient == null) {
            fail("Client not ready");
        }

        // Wait for the first change to be processed
        Thread.sleep(200);

        CountDownLatch updateLatch = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));
        Thread.sleep(100);
        pushToStreaming(streamingData, notification);
        mCustomSplitChangesResponse.set(IntegrationHelper.splitChangeWithReferencedRbs(5, 5));
        boolean updateAwaited = updateLatch.await(10, TimeUnit.SECONDS);
        if (!updateAwaited) {
            fail("SDK_UPDATE not received");
        }

        Thread.sleep(200);
        List<RuleBasedSegmentEntity> entities = mRoomDb.ruleBasedSegmentDao().getAll();
        List<String> names = entities.stream().map(RuleBasedSegmentEntity::getName).collect(Collectors.toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("rbs_test") && names.contains("new_rbs_test"));
    }

    private void successfulInstantUpdateTest(String rbsChange0, String expectedContents) throws IOException, InterruptedException {
        // Initialize a factory with RBS enabled
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, IntegrationHelper.dummyUserKey());
        if (readyClient == null) {
            fail("Client not ready");
        }

        // Wait for the first change to be processed
        Thread.sleep(200);

        // Push a split update through the streaming connection
        boolean updateProcessed = processUpdate(readyClient, streamingData, rbsChange0, expectedContents);

        assertTrue(updateProcessed);
    }

    @Nullable
    private SplitClient getReadyClient(
            Context context,
            SplitRoomDatabase splitRoomDatabase,
            BlockingQueue<String> streamingData, Key key) throws IOException, InterruptedException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .trafficType("user")
                .streamingEnabled(true)
                .enableDebug()
                .build();
        CountDownLatch authLatch = new CountDownLatch(1);
        Map<String, IntegrationHelper.ResponseClosure> responses = getStringResponseClosureMap(authLatch);

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses, streamingData);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                key,
                config,
                context,
                new HttpClientMock(httpResponseMockDispatcher),
                splitRoomDatabase, null, null, null);

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = splitFactory.client();
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);
        boolean authAwait = authLatch.await(5, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(streamingData);

        return (await && authAwait) ? client : null;
    }

    @NonNull
    private Map<String, IntegrationHelper.ResponseClosure> getStringResponseClosureMap(CountDownLatch authLatch) {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put(IntegrationHelper.ServicePath.SPLIT_CHANGES, (uri, httpMethod, body) -> {
            if (mCustomSplitChangesResponse.get() != null) {
                return new HttpResponseMock(200, mCustomSplitChangesResponse.get());
            }

            return new HttpResponseMock(200, IntegrationHelper.emptyTargetingRulesChanges(1, 1));
        });
        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyAllSegments()));
        responses.put("v2/auth", (uri, httpMethod, body) -> {
            authLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });
        return responses;
    }

    private boolean processUpdate(SplitClient client, LinkedBlockingDeque<String> streamingData, String change, String... expectedContents) throws InterruptedException {
        CountDownLatch updateLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));
        pushToStreaming(streamingData, change);
        boolean updateAwaited = updateLatch.await(10, TimeUnit.SECONDS);
        List<RuleBasedSegmentEntity> entities = mRoomDb.ruleBasedSegmentDao().getAll();
        if (!updateAwaited) {
            fail("SDK_UPDATE not received");
        }

        if (expectedContents == null || expectedContents.length == 0) {
            return !entities.isEmpty();
        }

        boolean contentMatches = true;
        for (String expected : expectedContents) {
            contentMatches = contentMatches && entities.size() == 1 && entities.get(0).getBody().contains(expected);
        }

        return contentMatches;
    }

    private static void pushToStreaming(LinkedBlockingDeque<String> streamingData, String message) {
        try {
            streamingData.put(message + "" + "\n");
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException ignored) {
        }
    }

    @NonNull
    private static String getBasicChange(String changeNumber, String pcn) {
        return rbsChange(changeNumber, pcn, "eyJuYW1lIjoicmJzX3Rlc3QiLCJzdGF0dXMiOiJBQ1RJVkUiLCJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiZXhjbHVkZWQiOnsia2V5cyI6W10sInNlZ21lbnRzIjpbXX0sImNvbmRpdGlvbnMiOlt7Im1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX19XX0=");
    }
}
