package tests.integration.largesegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.SegmentChangeDTO;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import tests.integration.shared.TestingData;
import tests.integration.shared.TestingHelper;

public class LargeSegmentsStreamingTest {

    public static final String SPLIT_CHANGES = "splitChanges";
    public static final String MY_SEGMENTS = "mySegments";
    public static final String AUTH = "v2/auth";
    public static final String SSE = "sse";
    private final FileHelper mFileHelper = new FileHelper();
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private Map<String, AtomicInteger> mEndpointHits;
    private Map<String, CountDownLatch> mLatches;
    private final AtomicInteger mMyLargeSegmentsStatusCode = new AtomicInteger(200);
    private final AtomicBoolean mRandomizeMyLargeSegments = new AtomicBoolean(false);
    private BlockingQueue<String> mStreamingData;

    @Before
    public void setUp() throws IOException, InterruptedException {
        mStreamingData = new LinkedBlockingQueue<>();
        mEndpointHits = new ConcurrentHashMap<>();
        mMyLargeSegmentsStatusCode.set(200);
        mRandomizeMyLargeSegments.set(false);
        initializeLatches();
    }

    @Test
    public void unboundedLargeSegmentsUpdateTriggersSdkUpdate() throws IOException, InterruptedException {
        TestSetup testSetup = getTestSetup();

        boolean mySegmentsAwait = mLatches.get(MY_SEGMENTS).await(10, TimeUnit.SECONDS);
        boolean splitsAwait = mLatches.get(SPLIT_CHANGES).await(10, TimeUnit.SECONDS);
        String initialSegmentList = testSetup.database.myLargeSegmentDao().getByUserKey(IntegrationHelper.dummyUserKey().matchingKey()).getSegmentList();
        mRandomizeMyLargeSegments.set(true);

        pushMyLargeSegmentsMessage(TestingData.largeSegmentsUnboundedNoCompression("100"));
        boolean updateAwait = testSetup.updateLatch.await(15, TimeUnit.SECONDS);

        assertTrue(testSetup.await);
        assertTrue(testSetup.authAwait);
        assertTrue(mySegmentsAwait);
        assertTrue(splitsAwait);
        assertTrue(updateAwait);
        assertEquals(2, mEndpointHits.get(SPLIT_CHANGES).get());
        assertEquals(3, mEndpointHits.get(MY_SEGMENTS).get());
        assertEquals("{\"segments\":[\"large-segment1\",\"large-segment2\",\"large-segment3\"],\"till\":9999999999999}", initialSegmentList);
        assertEquals(2, Json.fromJson(testSetup.database.myLargeSegmentDao().getByUserKey(IntegrationHelper.dummyUserKey().matchingKey()).getSegmentList(), SegmentChangeDTO.class).getMySegments().size());
    }

    @Test
    public void segmentRemovalTriggersSdkUpdateAndRemovesSegmentFromStorage() throws IOException, InterruptedException {
        TestSetup testSetup = getTestSetup();

        boolean mySegmentsAwait = mLatches.get(MY_SEGMENTS).await(10, TimeUnit.SECONDS);
        boolean splitsAwait = mLatches.get(SPLIT_CHANGES).await(10, TimeUnit.SECONDS);

        SplitRoomDatabase db = testSetup.database;
        String initialLargeSegmentsSize = db.myLargeSegmentDao()
                .getByUserKey(IntegrationHelper.dummyUserKey().matchingKey())
                .getSegmentList();

        pushMyLargeSegmentsMessage(TestingData.largeSegmentsRemoval());
        boolean updateAwait = testSetup.updateLatch.await(10, TimeUnit.SECONDS);

        assertTrue(testSetup.await);
        assertTrue(testSetup.authAwait);
        assertTrue(mySegmentsAwait);
        assertTrue(splitsAwait);
        assertTrue(updateAwait);
        assertEquals("{\"segments\":[\"large-segment1\",\"large-segment2\",\"large-segment3\"],\"till\":9999999999999}", initialLargeSegmentsSize);
        assertEquals("{\"segments\":[\"large-segment3\"],\"till\":1702507130121}", db.myLargeSegmentDao().getByUserKey(IntegrationHelper.dummyUserKey().matchingKey()).getSegmentList());
    }

    @NonNull
    private TestSetup getTestSetup() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        SplitRoomDatabase database = DatabaseHelper.getTestDatabase(mContext);
        SplitFactory factory = getFactory(database);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(latch));
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));
        boolean await = latch.await(5, TimeUnit.SECONDS);
        boolean authAwait = mLatches.get(AUTH).await(6, TimeUnit.SECONDS);

        // Wait for streaming connection
        mLatches.get(SSE).await(15, TimeUnit.SECONDS);

        // Keep alive on streaming channel confirms connection
        // so full sync is fired
        TestingHelper.pushKeepAlive(mStreamingData);
        return new TestSetup(updateLatch, database, await, authAwait);
    }

    private SplitFactory getFactory(SplitRoomDatabase database) throws IOException {
        if (database == null) {
            database = DatabaseHelper.getTestDatabase(mContext);
        }
        TestableSplitConfigBuilder configBuilder = new TestableSplitConfigBuilder()
                .streamingEnabled(true)
                .enableDebug();

        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                configBuilder.build(),
                mContext,
                new HttpClientMock(buildDispatcher()), database, null, null, null);
    }

    private HttpResponseMockDispatcher buildDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put(SPLIT_CHANGES, (path, query, body) -> {
            updateEndpointHit(SPLIT_CHANGES);
            return new HttpResponseMock(200, splitChangesLargeSegments(1602796638344L, 1602796638344L));
        });

        String key = IntegrationHelper.dummyUserKey().matchingKey();
        responses.put("mySegments/" + key, (path, query, body) -> {
            updateEndpointHit(MY_SEGMENTS);
            if (mMyLargeSegmentsStatusCode.get() != 200) {
                return new HttpResponseMock(mMyLargeSegmentsStatusCode.get());
            } else {
                String responseBody = IntegrationHelper.dummyMyUnifiedSegments();
                if (mRandomizeMyLargeSegments.get()) {
                    responseBody = IntegrationHelper.randomizedMyUnifiedSegments();
                }
                return new HttpResponseMock(200, responseBody);
            }
        });

        responses.put(AUTH, (path, query, body) -> {
            try {
                return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
            } finally {
                updateEndpointHit(AUTH);
            }
        });

        return IntegrationHelper.buildDispatcher(responses, mStreamingData, mLatches.get(SSE));
    }

    private String splitChangesLargeSegments(long since, long till) {
        String change = mFileHelper.loadFileContent(mContext, "split_changes_large_segments-0.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = since;
        parsedChange.till = till;

        return Json.toJson(parsedChange);
    }

    private void initializeLatches() {
        mLatches = new ConcurrentHashMap<>();
        mLatches.put(SPLIT_CHANGES, new CountDownLatch(2));
        mLatches.put(MY_SEGMENTS, new CountDownLatch(2));
        mLatches.put(AUTH, new CountDownLatch(1));
        mLatches.put(SSE, new CountDownLatch(1));
    }

    private void updateEndpointHit(String splitChanges) {
        if (mEndpointHits.containsKey(splitChanges)) {
            mEndpointHits.get(splitChanges).getAndIncrement();
        } else {
            mEndpointHits.put(splitChanges, new AtomicInteger(1));
        }

        if (mLatches.containsKey(splitChanges)) {
            mLatches.get(splitChanges).countDown();
        }
    }

    private void pushMyLargeSegmentsMessage(String msg) {
        String MSG_SEGMENT_UPDATE_TEMPLATE = "push_msg-largesegment_update.txt";
        BlockingQueue<String> queue = mStreamingData;
        String message = loadMockedData(MSG_SEGMENT_UPDATE_TEMPLATE);
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
        message = message.replace(TestingHelper.MSG_DATA_FIELD, msg);
        try {
            queue.put(message + "" + "\n");
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private String loadMockedData(String fileName) {
        return mFileHelper.loadFileContent(mContext, fileName);
    }

    private static class TestSetup {
        public final CountDownLatch updateLatch;
        public final SplitRoomDatabase database;
        public final boolean await;
        public final boolean authAwait;

        public TestSetup(CountDownLatch updateLatch, SplitRoomDatabase database, boolean await, boolean authAwait) {
            this.updateLatch = updateLatch;
            this.database = database;
            this.await = await;
            this.authAwait = authAwait;
        }
    }
}
