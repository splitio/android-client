package tests.integration.largesegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class LargeSegmentsTest extends LargeSegmentTestHelper {

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsTrue() {
        SplitFactory factory = getFactory(true, true);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(1, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void sdkReadyTimeoutIsEmittedWhenWaitForLargeSegmentsIsTrueAndSyncFails() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch sdkReadyTimeoutLatch = new CountDownLatch(1);
        mMyLargeSegmentsStatusCode.set(500);
        SplitFactory factory = getFactory(true, true, null, 2500, null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, TestingHelper.testTask(sdkReadyTimeoutLatch));
        boolean readyAwait = countDownLatch.await(5, TimeUnit.SECONDS);
        boolean sdkReadyTimeoutAwait = sdkReadyTimeoutLatch.await(5, TimeUnit.SECONDS);

        assertFalse(readyAwait);
        assertTrue(sdkReadyTimeoutAwait);
        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(2, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsFalse() {
        mMyLargeSegmentsDelay.set(4000);
        SplitFactory factory = getFactory(true, false);
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);


        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertNull(mEndpointHits.get("myLargeSegments"));
    }

    @Test
    public void sdkUpdateIsEmittedForLargeSegmentsWhenLargeSegmentsChange() throws InterruptedException {
        mRandomizeMyLargeSegments.set(true);
        mMyLargeSegmentsDelay.set(0L);
        CountDownLatch updateLatch = new CountDownLatch(3);
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitFactory factory = getFactory(true, true, 1, null, null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));

        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);
        boolean await = updateLatch.await(5, TimeUnit.SECONDS);


        assertTrue(readyAwait);
        assertTrue(await);
    }

    @Test
    public void noHitsToMyLargeSegmentsEndpointWhenLargeSegmentsAreDisabled() throws InterruptedException {
        mMyLargeSegmentsDelay.set(0L);
        SplitFactory factory = getFactory(false, true, 1, null, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        Thread.sleep(5000);


        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertNull(mEndpointHits.get("myLargeSegments"));
    }

    @Test
    public void onlyOneHitToLargeSegmentsWhenPollingIsEnabledAndEndpointFailsWith403() throws InterruptedException {
        mMyLargeSegmentsDelay.set(0L);
        mMyLargeSegmentsStatusCode.set(403);

        SplitFactory factory = getFactory(true, false, 3, null, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        Thread.sleep(5000);

        assertEquals(1, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void sdkReadyIsEmittedWhenWaitForLargeSegmentsIsTrueAndSyncFailsWith403Code() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch sdkReadyTimeoutLatch = new CountDownLatch(1);
        mMyLargeSegmentsStatusCode.set(403);
        SplitFactory factory = getFactory(true, true, null, 2500, null);
        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, TestingHelper.testTask(sdkReadyTimeoutLatch));
        boolean readyAwait = countDownLatch.await(5, TimeUnit.SECONDS);
        boolean sdkReadyTimeoutAwait = sdkReadyTimeoutLatch.await(5, TimeUnit.SECONDS);
        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(1, mEndpointHits.get("myLargeSegments").get());
        assertTrue(readyAwait);
        assertFalse(sdkReadyTimeoutAwait);

    }

    @Test
    public void multipleHitsToLargeSegmentsWhenWhenEndpointFailsWithErrorCodeDifferentThan403() throws InterruptedException {
        mLatches.put("myLargeSegments", new CountDownLatch(2));
        mMyLargeSegmentsDelay.set(0L);
        mMyLargeSegmentsStatusCode.set(500);

        SplitFactory factory = getFactory(true, false, null, null, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        mMyLargeSegmentsStatusCode.set(200);
        boolean hitsAwait = mLatches.get("myLargeSegments").await(10, TimeUnit.SECONDS);

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(2, mEndpointHits.get("myLargeSegments").get());
        assertTrue(hitsAwait);
    }

    @Test
    public void sdkReadyFromCacheIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsTrue() throws InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);
        // first, prepopulate local cache
        SplitFactory factory = getFactory(true, true, null, null, testDatabase);
        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        String firstEval = readyClient.getTreatment("ls_split");

        // make all api requests fail, we only want sdk_ready_from_cache
        mBrokenApi.set(true);

        factory = getFactory(true, false, null, 1000, testDatabase);
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

        SplitFactory factory = getFactory(true, true, null, null, testDatabase);
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        String[] largeSegments = testDatabase.myLargeSegmentDao()
                .getByUserKey(IntegrationHelper.dummyUserKey().matchingKey())
                .getSegmentList().split(",");
        assertEquals(2, largeSegments.length);
        assertEquals("large-segment1", largeSegments[0]);
        assertEquals("large-segment2", largeSegments[1]);
    }
}
