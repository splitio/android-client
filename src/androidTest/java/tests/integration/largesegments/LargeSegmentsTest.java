package tests.integration.largesegments;

import static org.junit.Assert.assertEquals;
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

import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class LargeSegmentsTest {

    private final FileHelper mFileHelper = new FileHelper();
    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private MockWebServer mWebServer;
    private Map<String, AtomicInteger> mEndpointHits;
    private Map<String, CountDownLatch> mLatches;
    private final AtomicLong mMySegmentsDelay = new AtomicLong(0L);
    private final AtomicLong mMyLargeSegmentsDelay = new AtomicLong(1000L);
    private final AtomicInteger mMyLargeSegmentsStatusCode = new AtomicInteger(200);
    private final AtomicBoolean mRandomizeMyLargeSegments = new AtomicBoolean(false);
    private final AtomicBoolean mBrokenApi = new AtomicBoolean(false);

    @Before
    public void setUp() throws IOException {
        mEndpointHits = new ConcurrentHashMap<>();
        mMySegmentsDelay.set(0L);
        mMyLargeSegmentsDelay.set(1000L);
        mMyLargeSegmentsStatusCode.set(200);
        mRandomizeMyLargeSegments.set(false);
        mBrokenApi.set(false);
        initializeLatches();

        mWebServer = new MockWebServer();
        mWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                System.out.println("Receiving request to " + request.getRequestUrl().toString());

                if (mBrokenApi.get()) {
                    return new MockResponse().setResponseCode(500);
                }

                if (request.getRequestUrl().encodedPathSegments().contains("splitChanges")) {
                    updateEndpointHit("splitChanges");
                    return new MockResponse().setResponseCode(200).setBody(splitChangesLargeSegments(1602796638344L, 1602796638344L));
                } else if (request.getRequestUrl().encodedPathSegments().contains("mySegments")) {
                    Thread.sleep(mMySegmentsDelay.get());
                    updateEndpointHit("mySegments");
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyMySegments());
                } else if (request.getRequestUrl().encodedPathSegments().contains("myLargeSegments")) {

                    Thread.sleep(mMyLargeSegmentsDelay.get());
                    updateEndpointHit("myLargeSegments");
                    if (mMyLargeSegmentsStatusCode.get() != 200) {
                        return new MockResponse().setResponseCode(mMyLargeSegmentsStatusCode.get());
                    } else {
                        String body = IntegrationHelper.dummyMyLargeSegments();
                        if (mRandomizeMyLargeSegments.get()) {
                            body = IntegrationHelper.randomizedMyLargeSegments();
                        }

                        return new MockResponse().setResponseCode(200).setBody(body);
                    }

                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        mWebServer.start();
    }

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsTrue() {
        SplitFactory factory = getFactory(true, true);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        factory.destroy();

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(0, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void sdkReadyTimeoutIsEmittedWhenWaitForLargeSegmentsIsTrueAndSyncFails() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean sdkReadyTimeout = new AtomicBoolean(false);
        mMyLargeSegmentsStatusCode.set(500);
        SplitFactory factory = getFactory(true, true, null, 2500);
        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, TestingHelper.testTask(countDownLatch));
        boolean readyAwait = countDownLatch.await(5, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        assertTrue(sdkReadyTimeout.get());
        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(1, mEndpointHits.get("myLargeSegments").get());

        factory.destroy();
    }

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsFalse() {
        mMyLargeSegmentsDelay.set(4000);
        SplitFactory factory = getFactory(true, false);
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        factory.destroy();

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
        SplitFactory factory = getFactory(true, true, 1, null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        client.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));

        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);
        boolean await = updateLatch.await(5, TimeUnit.SECONDS);

        factory.destroy();

        assertTrue(readyAwait);
        assertTrue(await);
    }

    @Test
    public void noHitsToMyLargeSegmentsEndpointWhenLargeSegmentsAreDisabled() throws InterruptedException {
        mMyLargeSegmentsDelay.set(0L);
        SplitFactory factory = getFactory(false, true, 1, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        Thread.sleep(5000);

        factory.destroy();

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertNull(mEndpointHits.get("myLargeSegments"));
    }

    @Test
    public void onlyOneHitToLargeSegmentsWhenPollingIsEnabledAndEndpointFailsWith403() throws InterruptedException {
        mMyLargeSegmentsDelay.set(0L);
        mMyLargeSegmentsStatusCode.set(403);

        SplitFactory factory = getFactory(true, false, 3, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        Thread.sleep(5000);

        assertEquals(1, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void multipleHitsToLargeSegmentsWhenWhenEndpointFailsWithErrorCodeDifferentThan403() throws InterruptedException {
        mLatches.put("myLargeSegments", new CountDownLatch(2));
        mMyLargeSegmentsDelay.set(0L);
        mMyLargeSegmentsStatusCode.set(500);

        SplitFactory factory = getFactory(true, false, null, null);

        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        mMyLargeSegmentsStatusCode.set(200);
        boolean hitsAwait = mLatches.get("myLargeSegments").await(10, TimeUnit.SECONDS);
        factory.destroy();

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(2, mEndpointHits.get("myLargeSegments").get());
        assertTrue(hitsAwait);
    }

    @Test
    public void sdkReadyFromCacheIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsTrue() throws InterruptedException {
        // first, prepopulate local cache
        SplitFactory factory = getFactory(true, true);
        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);
        factory.destroy();

        // make all api requests fail, we only want sdk_ready_from_cache
        mBrokenApi.set(true);

        factory = getFactory(true, false, null, 1000);
        CountDownLatch fromCacheLatch = new CountDownLatch(1);
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        factory.client().on(SplitEvent.SDK_READY_FROM_CACHE, TestingHelper.testTask(fromCacheLatch));
        factory.client().on(SplitEvent.SDK_READY_TIMED_OUT, TestingHelper.testTask(timeoutLatch));

        boolean fromCacheAwait = fromCacheLatch.await(5, TimeUnit.SECONDS);
        boolean timeoutAwait = timeoutLatch.await(5, TimeUnit.SECONDS);

        assertTrue(fromCacheAwait);
        assertTrue(timeoutAwait);

        factory.destroy();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.close();
    }

    private void initializeLatches() {
        mLatches = new ConcurrentHashMap<>();
        mLatches.put("splitChanges", new CountDownLatch(1));
        mLatches.put("mySegments", new CountDownLatch(1));
        mLatches.put("myLargeSegments", new CountDownLatch(1));
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

    private SplitFactory getFactory(boolean largeSegmentsEnabled, boolean waitForLargeSegments) {
        return getFactory(largeSegmentsEnabled, waitForLargeSegments, null, 2500);
    }

    private SplitFactory getFactory(boolean largeSegmentsEnabled,
                                    boolean waitForLargeSegments,
                                    Integer largeSegmentsRefreshRate,
                                    Integer ready) {
        TestableSplitConfigBuilder configBuilder = new TestableSplitConfigBuilder()
                .largeSegmentsEnabled(largeSegmentsEnabled)
                .waitForLargeSegments(waitForLargeSegments)
                .enableDebug()
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint("http://" + mWebServer.getHostName() + ":" + mWebServer.getPort())
                        .build());

        if (largeSegmentsRefreshRate != null) {
            configBuilder.streamingEnabled(false);
            configBuilder.largeSegmentsRefreshRate(largeSegmentsRefreshRate);
        }
        if (ready != null) {
            configBuilder.ready(ready);
        }
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                configBuilder.build(),
                mContext,
                null, null, null, null, null);
    }

    private SplitClient getReadyClient(String matchingKey, SplitFactory factory) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SplitClient client = factory.client(matchingKey);
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return client;
    }

    private String splitChangesLargeSegments(long since, long till) {
        String change = mFileHelper.loadFileContent(mContext, "split_changes_large_segments-0.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = since;
        parsedChange.till = till;

        return Json.toJson(parsedChange);
    }
}
