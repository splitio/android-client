package tests.integration.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.rbsChange;

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

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.utils.logger.Logger;
import tests.integration.shared.TestingHelper;

public class RuleBasedSegmentsIntegrationTest {

    private static final String rbsChange0 = rbsChange("2", "1", "eyJuYW1lIjoicmJzX3Rlc3QiLCJzdGF0dXMiOiJBQ1RJVkUiLCJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiZXhjbHVkZWQiOnsia2V5cyI6W10sInNlZ21lbnRzIjpbXX0sImNvbmRpdGlvbnMiOlt7Im1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX19XX0=");

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private SplitRoomDatabase mRoomDb;
    private final AtomicInteger mSplitChangesHits = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @Test
    public void testRuleBasedSegmentsSyncing() throws IOException, InterruptedException {
        // Initialize a factory with RBS enabled
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData);
        if (readyClient == null) {
            fail("Client not ready");
        }

        // Wait for the first change to be processed
        Thread.sleep(200);

        // Push a split update through the streaming connection
        boolean updateProcessed = processUpdate(readyClient, streamingData, rbsChange0, "\"name\":\"rbs_test\"");

        // Verify RBS sync hits
        assertTrue(updateProcessed);
        assertEquals(2, mSplitChangesHits.get());
    }

    @Nullable
    private SplitClient getReadyClient(
            Context context,
            SplitRoomDatabase splitRoomDatabase,
            BlockingQueue<String> streamingData) throws IOException, InterruptedException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .trafficType("client")
                .streamingEnabled(true)
                .enableDebug()
                .build();
        CountDownLatch authLatch = new CountDownLatch(1);
        Map<String, IntegrationHelper.ResponseClosure> responses = getStringResponseClosureMap(authLatch);

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses, streamingData);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
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
            mSplitChangesHits.incrementAndGet();
            return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(-1, 1));
        });
        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyAllSegments()));
        responses.put("v2/auth", (uri, httpMethod, body) -> {
            authLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });
        return responses;
    }

    private boolean processUpdate(SplitClient client, LinkedBlockingDeque<String> streamingData, String splitChange, String... expectedContents) throws InterruptedException {
        CountDownLatch updateLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));
        pushToStreaming(streamingData, splitChange);
        boolean updateAwaited = updateLatch.await(5, TimeUnit.SECONDS);
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
}
