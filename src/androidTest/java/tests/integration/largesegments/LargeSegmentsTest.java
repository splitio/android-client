package tests.integration.largesegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.exceptions.SplitInstantiationException;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class LargeSegmentsTest extends LargeSegmentTestHelper {

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSynced() {
        SplitFactory factory = getFactory();

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
    }

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsFalse() {
        mMySegmentsDelay.set(4000);
        SplitFactory factory = getFactory();
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);


        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
    }

    @Test
    public void sdkUpdateIsEmittedForLargeSegmentsWhenLargeSegmentsChange() throws InterruptedException {
        mMySegmentsDelay.set(0L);
        mRandomizeMyLargeSegments.set(true);
        CountDownLatch updateLatch = new CountDownLatch(3);
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitFactory factory = getFactory(1, null, null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));

        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);
        boolean await = updateLatch.await(5, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        assertTrue(await);
    }

    @Test
    public void sdkReadyFromCacheIsEmittedAfterLargeSegmentsAreSynced() throws InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);
        // first, prepopulate local cache
        SplitFactory factory = getFactory(null, null, testDatabase);
        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        String firstEval = readyClient.getTreatment("ls_split");

        // make all api requests fail, we only want sdk_ready_from_cache
        mBrokenApi.set(true);

        factory = getFactory(null, 1000, testDatabase);
        CountDownLatch fromCacheLatch = new CountDownLatch(1);
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY_FROM_CACHE, TestingHelper.testTask(fromCacheLatch));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, TestingHelper.testTask(timeoutLatch));

        boolean fromCacheAwait = fromCacheLatch.await(5, TimeUnit.SECONDS);
        boolean timeoutAwait = timeoutLatch.await(5, TimeUnit.SECONDS);

        String lsSplit = client.getTreatment("ls_split");

        assertTrue(fromCacheAwait);
        assertTrue(timeoutAwait);
        assertEquals("on", firstEval);
        assertEquals("on", lsSplit);

    }

    @Test
    public void successfulSyncOfLargeSegmentsContainsSegmentsInDatabase() {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = getFactory(null, null, testDatabase);
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        SegmentsChange largeSegments = Json.fromJson(testDatabase.myLargeSegmentDao()
                .getByUserKey(IntegrationHelper.dummyUserKey().matchingKey())
                .getSegmentList(), SegmentsChange.class);
        List<String> mySegments = largeSegments.getNames();
        assertEquals(3, mySegments.size());
        assertTrue(mySegments.contains("large-segment1") && mySegments.contains("large-segment2") && mySegments.contains("large-segment3"));
        assertEquals(9999999999999L, largeSegments.getChangeNumber().longValue());
    }

    @Test
    public void syncOfLargeSegmentsForMultiClient() throws InterruptedException {
        mRandomizeMyLargeSegments.set(true);
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);
        SplitFactory factory = getFactory(10, null, testDatabase);

        SplitClient client1 = factory.client();
        SplitClient client2 = factory.client("key2");

        CountDownLatch latch = new CountDownLatch(2);
        client1.on(SplitEvent.SDK_READY, TestingHelper.testTask(latch));
        client2.on(SplitEvent.SDK_READY, TestingHelper.testTask(latch));

        latch.await(10, TimeUnit.SECONDS);

        SegmentsChange segmentList1 = Json.fromJson(testDatabase.myLargeSegmentDao().getByUserKey("CUSTOMER_ID").getSegmentList(), SegmentsChange.class);
        SegmentsChange segmentList2 = Json.fromJson(testDatabase.myLargeSegmentDao().getByUserKey("key2").getSegmentList(), SegmentsChange.class);

        assertEquals(2, segmentList1.getNames().size());
        assertEquals(2, segmentList2.getNames().size());
        assertNotEquals(segmentList1,
                segmentList2);
        assertEquals(9999999999999L, segmentList1.getChangeNumber().longValue());
        assertEquals(9999999999999L, segmentList2.getChangeNumber().longValue());
    }

    @Test
    public void emptyMyLargeSegmentsSdkIsReady() throws InterruptedException {
        mMySegmentsDelay.set(0L);
        mEmptyMyLargeSegments.set(true);
        SplitFactory factory = getFactory(null, null, null);
        SplitClient client = factory.client();
        CountDownLatch readyLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertTrue(readyAwait);
    }

    @Test
    public void localhostModeIsReadyWhenWaitForLargeSegmentsIsTrue() throws SplitInstantiationException, InterruptedException {
        SplitFactory factory = SplitFactoryBuilder.build("localhost", IntegrationHelper.dummyUserKey(),
                new TestableSplitConfigBuilder().build(), mContext);

        CountDownLatch readyLatch = new CountDownLatch(1);
        factory.client().on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);

        assertTrue(await);
    }
}
