package tests.integration.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import tests.integration.shared.TestingHelper;

public class RuleBasedSegmentsIntegrationTest {

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
        String splitUpdateMessage = splitChangeV2("2", "1", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiaWQiOiJkNDMxY2RkMC1iMGJlLTExZWEtOGE4MC0xNjYwYWRhOWNlMzkiLCJuYW1lIjoibWF1cm9famF2YSIsInRyYWZmaWNBbGxvY2F0aW9uIjoxMDAsInRyYWZmaWNBbGxvY2F0aW9uU2VlZCI6LTkyMzkxNDkxLCJzZWVkIjotMTc2OTM3NzYwNCwic3RhdHVzIjoiQUNUSVZFIiwia2lsbGVkIjpmYWxzZSwiZGVmYXVsdFRyZWF0bWVudCI6Im9mZiIsImNoYW5nZU51bWJlciI6MTYwMjc5OTYzODM0NCwiYWxnbyI6MiwiY29uZmlndXJhdGlvbnMiOnt9LCJzZXRzIjpbXSwiY29uZGl0aW9ucyI6W3siY29uZGl0aW9uVHlwZSI6IldISVRFTElTVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJtYXRjaGVyVHlwZSI6IldISVRFTElTVCIsIm5lZ2F0ZSI6ZmFsc2UsIndoaXRlbGlzdE1hdGNoZXJEYXRhIjp7IndoaXRlbGlzdCI6WyJhZG1pbiIsIm1hdXJvIiwibmljbyJdfX1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfV0sImxhYmVsIjoid2hpdGVsaXN0ZWQifSx7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoidXNlciJ9LCJtYXRjaGVyVHlwZSI6IklOX1NFR01FTlQiLCJuZWdhdGUiOmZhbHNlLCJ1c2VyRGVmaW5lZFNlZ21lbnRNYXRjaGVyRGF0YSI6eyJzZWdtZW50TmFtZSI6Im1hdXItMiJ9fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6IlY0Iiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJ2NSIsInNpemUiOjB9XSwibGFiZWwiOiJpbiBzZWdtZW50IG1hdXItMiJ9LHsiY29uZGl0aW9uVHlwZSI6IlJPTExPVVQiLCJtYXRjaGVyR3JvdXAiOnsiY29tYmluZXIiOiJBTkQiLCJtYXRjaGVycyI6W3sia2V5U2VsZWN0b3IiOnsidHJhZmZpY1R5cGUiOiJ1c2VyIn0sIm1hdGNoZXJUeXBlIjoiQUxMX0tFWVMiLCJuZWdhdGUiOmZhbHNlfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6IlY0Iiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJ2NSIsInNpemUiOjB9XSwibGFiZWwiOiJkZWZhdWx0IHJ1bGUifV19");
        boolean updateProcessed = processUpdate(readyClient, streamingData, splitUpdateMessage);

        // Verify RBS sync hits
        assertNotNull(readyClient);
        assertTrue(updateProcessed);
        assertEquals(1, mSplitChangesHits.get());
    }

    @Nullable
    private SplitClient getReadyClient(
            Context context,
            SplitRoomDatabase splitRoomDatabase,
            BlockingQueue<String> streamingData) throws IOException, InterruptedException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .trafficType("client")
                .streamingEnabled(true)
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
        boolean updateAwaited = updateLatch.await(2, TimeUnit.SECONDS);
        List<SplitEntity> entities = mRoomDb.splitDao().getAll();

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
