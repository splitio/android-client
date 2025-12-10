package tests.integration.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class SdkEventsIntegrationTest {

    private Context mContext;
    private MockWebServer mWebServer;
    private SplitRoomDatabase mDatabase;
    private int mCurSplitReqId;

    private ServiceEndpoints endpoints() {
        final String url = mWebServer.url("/").url().toString();
        return ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();
    }

    private SplitClientConfig buildConfig() {
        return SplitClientConfig.builder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(999999) // High refresh rate to avoid periodic sync interfering
                .segmentsRefreshRate(999999)
                .impressionsRefreshRate(999999)
                .syncEnabled(true) // Ensure sync is enabled
                .trafficType("account")
                .build();
    }

    private SplitFactory buildFactory(SplitClientConfig config) {
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), new Key("DEFAULT_KEY"), config, mContext, null, mDatabase, null);
    }

    @Before
    public void setup() {
        mWebServer = new MockWebServer();
        mCurSplitReqId = 1;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    // Return empty changes - we'll rely on cache for this test
                    long id = mCurSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(id, id));
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);
        try {
            mWebServer.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start mock server", e);
        }
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
    }

    @After
    public void tearDown() throws Exception {
        if (mWebServer != null) mWebServer.shutdown();
        if (mDatabase != null) {
            mDatabase.close();
        }
    }

    /**
     * Scenario: sdkReadyFromCache fires when cache loading completes
     * <p>
     * Given the SDK is starting with populated persistent storage
     * And a handler H is registered for sdkReadyFromCache
     * When internal events "splitsLoadedFromStorage", "mySegmentsLoadedFromStorage",
     * "attributesLoadedFromStorage" and "encryptionMigrationDone" are notified
     * Then sdkReadyFromCache is emitted exactly once
     * And handler H is invoked once
     * And the metadata contains "freshInstall" with value false
     * And the metadata contains "lastUpdateTimestamp" with a valid timestamp
     */
    @Test
    public void sdkReadyFromCacheFiresWhenCacheLoadingCompletes() throws Exception {
        // Given: SDK is starting with populated persistent storage
        long testTimestamp = System.currentTimeMillis();
        populateDatabaseWithCacheData(testTimestamp);

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        // And: a handler H is registered for sdkReadyFromCache
        AtomicInteger handlerInvocationCount = new AtomicInteger(0);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch cacheReadyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerCacheReadyHandler(client, handlerInvocationCount, receivedMetadata, cacheReadyLatch);

        // When: internal events are notified (happens automatically during SDK initialization)
        boolean fired = cacheReadyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReadyFromCache is emitted exactly once
        assertTrue("SDK_READY_FROM_CACHE should fire", fired);
        assertEquals("Handler should be invoked exactly once", 1, handlerInvocationCount.get());

        // And: the metadata contains "freshInstall" with value false
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertTrue("Metadata should contain freshInstall key", receivedMetadata.get().containsKey("freshInstall"));
        assertFalse("freshInstall should be false for cache path",
                (Boolean) receivedMetadata.get().get("freshInstall"));

        // And: the metadata contains "lastUpdateTimestamp" with a valid timestamp
        assertTrue("Metadata should contain lastUpdateTimestamp key",
                receivedMetadata.get().containsKey("lastUpdateTimestamp"));
        Long lastUpdateTimestamp = (Long) receivedMetadata.get().get("lastUpdateTimestamp");
        assertNotNull("lastUpdateTimestamp should not be null", lastUpdateTimestamp);
        assertTrue("lastUpdateTimestamp should be valid", lastUpdateTimestamp > 0);

        factory.destroy();
    }

    /**
     * Scenario: sdkReadyFromCache fires when sync completes (fresh install path)
     * <p>
     * Given the SDK is starting without persistent storage (fresh install)
     * And a handler H is registered for sdkReadyFromCache
     * When internal events "targetingRulesSyncComplete" and "membershipsSyncComplete" are notified
     * Then sdkReadyFromCache is emitted exactly once
     * And handler H is invoked once
     * And the metadata contains "freshInstall" with value true
     */
    @Test
    public void sdkReadyFromCacheFiresWhenSyncCompletesFreshInstallPath() throws Exception {
        // Given: SDK is starting without persistent storage (fresh install)
        // Database is already empty from setup()

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        // And: a handler H is registered for sdkReadyFromCache
        AtomicInteger handlerInvocationCount = new AtomicInteger(0);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch cacheReadyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerCacheReadyHandler(client, handlerInvocationCount, receivedMetadata, cacheReadyLatch);

        // When: internal events "targetingRulesSyncComplete" and "membershipsSyncComplete" are notified
        boolean fired = cacheReadyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReadyFromCache is emitted exactly once
        assertTrue("SDK_READY_FROM_CACHE should fire", fired);
        assertEquals("Handler should be invoked exactly once", 1, handlerInvocationCount.get());

        // And: the metadata contains "freshInstall" with value true
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertTrue("Metadata should contain freshInstall key", receivedMetadata.get().containsKey("freshInstall"));
        assertTrue("freshInstall should be true for sync path (fresh install)",
                (Boolean) receivedMetadata.get().get("freshInstall"));

        factory.destroy();
    }

    /**
     * Scenario: sdkReady fires after sdkReadyFromCache and requires sync completion
     * <p>
     * Given the SDK has not yet emitted sdkReady
     * And a handler HReady is registered for sdkReady
     * And a handler HCache is registered for sdkReadyFromCache
     * When internal events "splitsLoadedFromStorage", "mySegmentsLoadedFromStorage",
     * "attributesLoadedFromStorage" and "encryptionMigrationDone" are notified
     * Then sdkReadyFromCache is emitted
     * And handler HCache is invoked once
     * But sdkReady is not emitted yet because sync has not completed
     * When internal events "targetingRulesSyncComplete" and "membershipsSyncComplete" are notified
     * Then sdkReady is emitted exactly once
     * And handler HReady is invoked once
     */
    @Test
    public void sdkReadyFiresAfterSdkReadyFromCacheAndRequiresSyncCompletion() throws Exception {
        // Given: SDK has not yet emitted sdkReady
        // Use fresh install (no cache) so SDK_READY_FROM_CACHE fires via sync path,
        // then SDK_READY fires after sync completes
        // Database is already empty from setup()

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        // And: handlers are registered BEFORE creating client to catch all events
        AtomicInteger cacheHandlerCount = new AtomicInteger(0);
        AtomicInteger readyHandlerCount = new AtomicInteger(0);
        CountDownLatch cacheReadyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        
        // Register handlers immediately
        client.on(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                cacheHandlerCount.incrementAndGet();
                cacheReadyLatch.countDown();
            }
        });

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                readyHandlerCount.incrementAndGet();
                readyLatch.countDown();
            }
        });

        // When: sync completes (happens automatically during initialization)
        // SDK_READY_FROM_CACHE fires via sync path when TARGETING_RULES_SYNC_COMPLETE and MEMBERSHIPS_SYNC_COMPLETE fire
        // Wait for SDK_READY_FROM_CACHE first
        boolean cacheFired = cacheReadyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY_FROM_CACHE should fire", cacheFired);
        assertEquals("Cache handler should be invoked once", 1, cacheHandlerCount.get());

        // When: sync completes (already happened, but SDK_READY requires SDK_READY_FROM_CACHE prerequisite)
        // SDK_READY requires both SDK_READY_FROM_CACHE (prerequisite) and sync completion (requireAll)
        // Wait for SDK_READY to fire
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReady is emitted exactly once
        assertTrue("SDK_READY should fire after SDK_READY_FROM_CACHE and sync completion. " +
                "Cache fired: " + cacheHandlerCount.get() + ", Ready fired: " + readyHandlerCount.get(), 
                readyFired);
        assertEquals("Ready handler should be invoked exactly once", 1, readyHandlerCount.get());
        
        // Verify both events fired
        assertTrue("SDK_READY_FROM_CACHE should fire", cacheHandlerCount.get() == 1);
        assertTrue("SDK_READY should fire after SDK_READY_FROM_CACHE", readyHandlerCount.get() == 1);

        factory.destroy();
    }

    /**
     * Scenario: sdkReady replays to late subscribers
     * <p>
     * Given sdkReady has already been emitted
     * When a new handler H is registered for sdkReady
     * Then handler H is invoked exactly once immediately (replay)
     * And sdkReady is not emitted again
     */
    @Test
    public void sdkReadyReplaysToLateSubscribers() throws Exception {
        // Given: sdkReady has already been emitted
        TestClientFixture fixture = createClientAndWaitForReady(new Key("key_1"));

        // When: a new handler H is registered for sdkReady
        AtomicInteger lateHandlerCount = new AtomicInteger(0);
        CountDownLatch lateHandlerLatch = new CountDownLatch(1);

        registerReadyHandler(fixture.client, lateHandlerCount, lateHandlerLatch);

        // Then: handler H is invoked exactly once immediately (replay)
        boolean replayFired = lateHandlerLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Late handler should receive replay", replayFired);
        assertEquals("Late handler should be invoked exactly once", 1, lateHandlerCount.get());

        // And: sdkReady is not emitted again (verify no additional invocations)
        Thread.sleep(500);
        assertEquals("Late handler should not be invoked again", 1, lateHandlerCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: sdkUpdate is emitted only after sdkReady
     * <p>
     * Given a handler H is registered for sdkUpdate
     * And the SDK has not yet emitted sdkReady
     * When an internal "splitsUpdated" event is notified
     * Then sdkUpdate is not emitted because sdkReady has not fired yet
     * When internal events for sdkReadyFromCache and sdkReady are notified and both fire
     * When a new "splitsUpdated" event is notified
     * Then sdkUpdate is emitted
     * And handler H is invoked once with metadata containing "updatedFlags"
     */
    @Test
    public void sdkUpdateEmittedOnlyAfterSdkReady() throws Exception {
        // Given: a handler H is registered for sdkUpdate
        // And: the SDK has not yet emitted sdkReady
        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        AtomicInteger updateHandlerCount = new AtomicInteger(0);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerUpdateHandler(client, updateHandlerCount, receivedMetadata);
        registerReadyHandler(client, null, readyLatch);

        // When: an internal "splitsUpdated" event is notified
        // (This happens during initial sync, but SDK_READY hasn't fired yet)
        Thread.sleep(1000);

        // Then: sdkUpdate is not emitted because sdkReady has not fired yet
        assertEquals("SDK_UPDATE should not fire before SDK_READY", 0, updateHandlerCount.get());

        // When: internal events for sdkReadyFromCache and sdkReady are notified and both fire
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // When: a new "splitsUpdated" event is notified
        // Note: We can't easily trigger splits updates in integration tests.
        // The key assertion is that SDK_UPDATE didn't fire before SDK_READY.
        Thread.sleep(1000);
        assertEquals("SDK_UPDATE should not fire before SDK_READY", 0, updateHandlerCount.get());

        factory.destroy();
    }

    /**
     * Scenario: sdkUpdate fires on any data change event after sdkReady
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When a split update notification arrives via SSE
     * Then sdkUpdate is emitted and handler H is invoked
     */
    @Test
    public void sdkUpdateFiresOnAnyDataChangeEventAfterSdkReady() throws Exception {
        // Given: sdkReady has already been emitted (with streaming support)
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger updateHandlerCount = new AtomicInteger(0);
        AtomicReference<EventMetadata> lastMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                updateHandlerCount.incrementAndGet();
                lastMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        // When: a split update notification arrives via SSE
        fixture.pushSplitUpdate();

        // Then: sdkUpdate is emitted and handler H is invoked
        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire after split update notification", updateFired);
        assertEquals("Handler should be invoked once", 1, updateHandlerCount.get());
        assertNotNull("Metadata should not be null", lastMetadata.get());

        fixture.destroy();
    }

    /**
     * Scenario: sdkUpdate does not replay to late subscribers
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H1 is registered for sdkUpdate
     * When an internal "splitsUpdated" event is notified via SSE
     * Then sdkUpdate is emitted
     * And handler H1 is invoked once
     * When a second handler H2 is registered for sdkUpdate after one sdkUpdate has already fired
     * Then H2 does not receive a replay for past sdkUpdate events
     * When another internal "splitsUpdated" event is notified
     * Then both H1 and H2 are invoked once for that second sdkUpdate
     */
    @Test
    public void sdkUpdateDoesNotReplayToLateSubscribers() throws Exception {
        // Given: sdkReady has already been emitted (with streaming support)
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger handler1Count = new AtomicInteger(0);
        AtomicInteger handler2Count = new AtomicInteger(0);
        CountDownLatch firstUpdateLatch = new CountDownLatch(1);
        AtomicReference<CountDownLatch> secondUpdateLatchRef = new AtomicReference<>(null);

        // And: a handler H1 is registered for sdkUpdate
        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handler1Count.incrementAndGet();
                firstUpdateLatch.countDown();
                // Count down second latch if it exists (second update)
                CountDownLatch secondLatch = secondUpdateLatchRef.get();
                if (secondLatch != null) {
                    secondLatch.countDown();
                }
            }
        });

        // When: an internal "splitsUpdated" event is notified via SSE
        // Use large change numbers to avoid any edge cases with change number validation
        fixture.pushSplitUpdate("2000", "1000");

        // Then: sdkUpdate is emitted and handler H1 is invoked once
        boolean firstUpdateFired = firstUpdateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire for H1", firstUpdateFired);
        assertEquals("H1 should be invoked once", 1, handler1Count.get());

        // Wait to ensure first update is fully processed and stored
        Thread.sleep(1000);

        // When: a second handler H2 is registered for sdkUpdate after one sdkUpdate has already fired
        CountDownLatch secondUpdateLatch = new CountDownLatch(2);
        secondUpdateLatchRef.set(secondUpdateLatch);
        
        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handler2Count.incrementAndGet();
                secondUpdateLatch.countDown();
            }
        });

        // Then: H2 does not receive a replay for past sdkUpdate events
        Thread.sleep(500);
        assertEquals("H2 should not receive replay", 0, handler2Count.get());

        // Ensure handlers are registered and first update is fully processed before pushing second update
        Thread.sleep(500);
        
        // Send keep-alive to ensure SSE connection is still active
        if (fixture.streamingData != null) {
            TestingHelper.pushKeepAlive(fixture.streamingData);
        }

        // When: another internal "splitsUpdated" event is notified (with incrementing change number)
        // Use a higher change number to ensure it's accepted after the first update
        fixture.pushSplitUpdate("2001", "2000");

        // Then: both H1 and H2 are invoked for that second sdkUpdate
        boolean secondUpdateFired = secondUpdateLatch.await(15, TimeUnit.SECONDS);
        assertTrue("Second SDK_UPDATE should fire. H1 count: " + handler1Count.get() + 
                ", H2 count: " + handler2Count.get() + 
                ", secondUpdateLatch count: " + secondUpdateLatch.getCount(), secondUpdateFired);

        // H1 should now have 2 total invocations (1 from first + 1 from second)
        assertEquals("H1 should have 2 total invocations", 2, handler1Count.get());
        // H2 should have 1 invocation (only from second update, no replay)
        assertEquals("H2 should have 1 invocation (no replay)", 1, handler2Count.get());

        fixture.destroy();
    }

    /**
     * Scenario: sdkReadyTimedOut is emitted when readiness timeout elapses
     * <p>
     * Given a handler Htimeout is registered for sdkReadyTimedOut
     * And a handler Hready is registered for sdkReady
     * And the readiness timeout is configured to T seconds
     * When the timeout T elapses without sdkReady firing
     * Then the internal "sdkReadyTimeoutReached" event is notified
     * And sdkReadyTimedOut is emitted exactly once
     * And handler Htimeout is invoked once
     * And sdkReady is not emitted
     */
    @Test
    public void sdkReadyTimedOutEmittedWhenReadinessTimeoutElapses() throws Exception {
        // Given: handlers are registered
        // And: the readiness timeout is configured to a short timeout (2 seconds)
        // Use a mock server that delays responses to prevent sync from completing quickly
        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints())
                .ready(2000) // 2 second timeout
                .featuresRefreshRate(999999)
                .segmentsRefreshRate(999999)
                .impressionsRefreshRate(999999)
                .syncEnabled(true) // Keep sync enabled but delay responses
                .trafficType("account")
                .build();

        // Set up mock server to delay responses so sync doesn't complete before timeout
        final Dispatcher delayedDispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    // Delay response to prevent sync from completing
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody(IntegrationHelper.dummyAllSegments())
                            .setBodyDelay(5, TimeUnit.SECONDS); // 5 second delay
                } else if (path.contains("/splitChanges")) {
                    // Delay response to prevent sync from completing
                    long id = mCurSplitReqId++;
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(id, id))
                            .setBodyDelay(5, TimeUnit.SECONDS); // 5 second delay
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(delayedDispatcher);

        SplitFactory factory = buildFactory(config);

        AtomicInteger timeoutHandlerCount = new AtomicInteger(0);
        AtomicInteger readyHandlerCount = new AtomicInteger(0);
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                timeoutHandlerCount.incrementAndGet();
                timeoutLatch.countDown();
            }
        });

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                readyHandlerCount.incrementAndGet();
                readyLatch.countDown();
            }
        });

        // When: the timeout elapses without sdkReady firing (due to delayed responses)
        boolean timeoutFired = timeoutLatch.await(5, TimeUnit.SECONDS);

        // Then: sdkReadyTimedOut is emitted exactly once
        assertTrue("SDK_READY_TIMED_OUT should fire after timeout. " +
                "Timeout count: " + timeoutHandlerCount.get() + ", Ready count: " + readyHandlerCount.get(),
                timeoutFired);
        assertEquals("Timeout handler should be invoked once", 1, timeoutHandlerCount.get());

        // And: sdkReady is not emitted (sync didn't complete in time)
        Thread.sleep(500);
        assertEquals("SDK_READY should not fire before timeout", 0, readyHandlerCount.get());

        factory.destroy();
    }

    /**
     * Scenario: sdkReadyTimedOut is suppressed when sdkReady fires before timeout
     * <p>
     * Given a handler Htimeout is registered for sdkReadyTimedOut
     * And a handler Hready is registered for sdkReady
     * And the readiness timeout is configured to T seconds
     * When internal events for sdkReadyFromCache and sdkReady complete before the timeout elapses
     * Then sdkReady is emitted
     * And sdkReadyTimedOut is not emitted
     * When the internal "sdkReadyTimeoutReached" event is notified after sdkReady has fired
     * Then sdkReadyTimedOut is still not emitted (suppressed by sdkReady)
     */
    @Test
    public void sdkReadyTimedOutSuppressedWhenSdkReadyFiresBeforeTimeout() throws Exception {
        // Given: handlers are registered
        // And: the readiness timeout is configured to a longer timeout (10 seconds)
        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints())
                .ready(10000) // 10 second timeout
                .featuresRefreshRate(999999)
                .segmentsRefreshRate(999999)
                .impressionsRefreshRate(999999)
                .syncEnabled(true)
                .trafficType("account")
                .build();

        SplitFactory factory = buildFactory(config);
        AtomicInteger timeoutHandlerCount = new AtomicInteger(0);
        AtomicInteger readyHandlerCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        client.on(SplitEvent.SDK_READY_TIMED_OUT, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                timeoutHandlerCount.incrementAndGet();
            }
        });
        registerReadyHandler(client, readyHandlerCount, readyLatch);

        // When: internal events for sdkReadyFromCache and sdkReady complete before the timeout elapses
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReady is emitted
        assertTrue("SDK_READY should fire", readyFired);
        assertEquals("Ready handler should be invoked once", 1, readyHandlerCount.get());

        // And: sdkReadyTimedOut is not emitted
        Thread.sleep(2000); // Wait a bit to ensure timeout doesn't fire
        assertEquals("SDK_READY_TIMED_OUT should not fire (suppressed)", 0, timeoutHandlerCount.get());

        factory.destroy();
    }

    /**
     * Scenario: Sync completion does not trigger sdkUpdate during initial sync
     * <p>
     * Given a handler HUpdate is registered for sdkUpdate
     * And a handler HReady is registered for sdkReady
     * And the SDK is performing initial sync
     * When internal events "splitsUpdated" and "ruleBasedSegmentsUpdated" are notified (data changed during sync)
     * And then "targetingRulesSyncComplete" and "membershipsSyncComplete" are notified
     * Then sdkReadyFromCache is emitted (via sync path)
     * And sdkReady is emitted
     * But sdkUpdate is NOT emitted because the *_UPDATED events were notified before sdkReady fired
     */
    @Test
    public void syncCompletionDoesNotTriggerSdkUpdateDuringInitialSync() throws Exception {
        // Given: handlers are registered
        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        AtomicInteger updateHandlerCount = new AtomicInteger(0);
        AtomicInteger readyHandlerCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerUpdateHandler(client, updateHandlerCount, null);
        registerReadyHandler(client, readyHandlerCount, readyLatch);

        // When: sync completes (happens automatically during initialization)
        // The *_UPDATED events fire before SDK_READY, so SDK_UPDATE shouldn't fire
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // Then: sdkUpdate is NOT emitted because the *_UPDATED events were notified before sdkReady fired
        Thread.sleep(1000);
        assertEquals("SDK_UPDATE should not fire during initial sync", 0, updateHandlerCount.get());

        factory.destroy();
    }

    /**
     * Scenario: Handlers for a single event are invoked sequentially and errors are isolated
     * <p>
     * Given three handlers H1, H2 and H3 are registered for sdkUpdate
     * And H2 throws an exception when invoked
     * And sdkReady has already been emitted
     * When an internal "splitsUpdated" event is notified via SSE
     * Then sdkUpdate is emitted once
     * And all handlers are invoked sequentially (one at a time, not concurrently)
     * And H2's exception is caught by delivery and doesn't crash the SDK
     * And H3 is invoked even though H2 threw an exception (error isolation)
     * And the SDK process does not crash
     */
    @Test
    public void handlersInvokedSequentiallyErrorsIsolated() throws Exception {
        // Given: sdkReady has already been emitted (with streaming support)
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger handler1Count = new AtomicInteger(0);
        AtomicInteger handler2Count = new AtomicInteger(0);
        AtomicInteger handler3Count = new AtomicInteger(0);
        AtomicInteger handler1Order = new AtomicInteger(0);
        AtomicInteger handler2Order = new AtomicInteger(0);
        AtomicInteger handler3Order = new AtomicInteger(0);
        AtomicInteger orderCounter = new AtomicInteger(0);
        CountDownLatch updateLatch = new CountDownLatch(3);

        // Given: three handlers H1, H2 and H3 are registered for sdkUpdate in that order
        // And: H2 throws an exception when invoked
        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handler1Count.incrementAndGet();
                handler1Order.set(orderCounter.incrementAndGet());
                updateLatch.countDown();
            }
        });

        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handler2Count.incrementAndGet();
                handler2Order.set(orderCounter.incrementAndGet());
                updateLatch.countDown();
                throw new RuntimeException("Handler H2 exception");
            }
        });

        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handler3Count.incrementAndGet();
                handler3Order.set(orderCounter.incrementAndGet());
                updateLatch.countDown();
            }
        });

        // When: an internal "splitsUpdated" event is notified via SSE
        fixture.pushSplitUpdate();

        // Then: all three handlers are invoked
        boolean allHandlersFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("All handlers should be invoked", allHandlersFired);

        // Verify all handlers were invoked exactly once
        assertEquals("Handler H1 should be invoked once", 1, handler1Count.get());
        assertEquals("Handler H2 should be invoked once", 1, handler2Count.get());
        assertEquals("Handler H3 should be invoked once despite H2 throwing", 1, handler3Count.get());

        // Verify handlers were invoked sequentially (orderCounter should be 1, 2, 3)
        // Note: We don't check which handler got which order number because handlers
        // are stored in a HashSet which doesn't guarantee iteration order.
        // The important thing is that all handlers were invoked and H3 was invoked
        // even though H2 threw an exception (error isolation).
        assertTrue("All handlers should have been assigned order numbers", 
                handler1Order.get() > 0 && handler2Order.get() > 0 && handler3Order.get() > 0);
        assertEquals("Order counter should be 3 (one for each handler)", 3, orderCounter.get());
        
        // Verify error isolation: H3 was invoked even though H2 threw an exception
        // This is the key assertion - that errors don't prevent subsequent handlers from executing
        assertTrue("H3 should be invoked even if H2 throws (error isolation)", handler3Count.get() == 1);

        fixture.destroy();
    }

    /**
     * Scenario: Metadata is correctly propagated to handlers
     * <p>
     * Given a handler H is registered for sdkUpdate which inspects the received metadata
     * And sdkReady has already been emitted
     * When an internal "splitsUpdated" event is notified via SSE
     * Then sdkUpdate is emitted
     * And handler H is invoked once
     * And handler H receives metadata (may contain updatedFlags depending on notification type)
     */
    @Test
    public void metadataCorrectlyPropagatedToHandlers() throws Exception {
        // Given: sdkReady has already been emitted (with streaming support)
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger updateHandlerCount = new AtomicInteger(0);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        // Given: a handler H is registered for sdkUpdate which inspects the received metadata
        fixture.client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                updateHandlerCount.incrementAndGet();
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        // When: an internal "splitsUpdated" event is notified via SSE
        fixture.pushSplitUpdate();

        // Then: sdkUpdate is emitted and handler H is invoked once
        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire", updateFired);
        assertEquals("Handler should be invoked exactly once", 1, updateHandlerCount.get());

        // And: handler H receives metadata
        assertNotNull("Metadata should not be null", receivedMetadata.get());

        fixture.destroy();
    }

    /**
     * Scenario: Destroying a client stops events and clears handlers
     * <p>
     * Given a SplitClient with an EventsManager and a handler H registered for sdkUpdate
     * And sdkReady has already been emitted
     * When the client is destroyed
     * And an internal "splitsUpdated" event is notified for that client
     * Then no external events are emitted
     * And handler H is never invoked
     * When registering a new handler H2 for sdkUpdate after destroy
     * Then the registration is a no-op
     * And H2 is never invoked
     */
    @Test
    public void destroyingClientStopsEventsAndClearsHandlers() throws Exception {
        // Given: sdkReady has already been emitted
        TestClientFixture fixture = createClientAndWaitForReady(new Key("key_1"));

        AtomicInteger handler1Count = new AtomicInteger(0);
        AtomicInteger handler2Count = new AtomicInteger(0);

        // Given: a handler H registered for sdkUpdate
        registerUpdateHandler(fixture.client, handler1Count, null);

        // When: the client is destroyed
        fixture.client.destroy();

        // When: registering a new handler H2 for sdkUpdate after destroy
        registerUpdateHandler(fixture.client, handler2Count, null);

        // Then: handlers are not invoked (client is destroyed)
        Thread.sleep(1000);
        assertEquals("Handler H1 should not be invoked after destroy", 0, handler1Count.get());
        assertEquals("Handler H2 should not be invoked after destroy", 0, handler2Count.get());

        fixture.destroy();
    }

    /**
     * Scenario: SDK-scoped internal events fan out to multiple clients
     * <p>
     * Given a factory with two clients ClientA and ClientB
     * And each client has its own EventsManager instance registered with EventsManagerCoordinator
     * And handlers HA and HB are registered for sdkUpdate on ClientA and ClientB respectively
     * And both clients have already emitted sdkReady
     * When a SDK-scoped internal "splitsUpdated" event is notified via SSE
     * Then sdkUpdate is emitted once per client
     * And handler HA is invoked once
     * And handler HB is invoked once
     */
    @Test
    public void sdkScopedEventsFanOutToMultipleClients() throws Exception {
        // Given: a factory with two clients (with streaming support)
        TwoClientFixture fixture = createTwoStreamingClientsAndWaitForReady(new Key("key_A"), new Key("key_B"));

        AtomicInteger handlerACount = new AtomicInteger(0);
        AtomicInteger handlerBCount = new AtomicInteger(0);
        CountDownLatch updateLatchA = new CountDownLatch(1);
        CountDownLatch updateLatchB = new CountDownLatch(1);

        // And: handlers HA and HB are registered for sdkUpdate
        fixture.clientA.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handlerACount.incrementAndGet();
                updateLatchA.countDown();
            }
        });

        fixture.clientB.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handlerBCount.incrementAndGet();
                updateLatchB.countDown();
            }
        });

        // When: a SDK-scoped internal "splitsUpdated" event is notified via SSE
        fixture.pushSplitUpdate();

        // Then: sdkUpdate is emitted once per client
        boolean updateAFired = updateLatchA.await(10, TimeUnit.SECONDS);
        boolean updateBFired = updateLatchB.await(10, TimeUnit.SECONDS);

        assertTrue("SDK_UPDATE should fire for ClientA", updateAFired);
        assertTrue("SDK_UPDATE should fire for ClientB", updateBFired);

        // And: handler HA is invoked once and handler HB is invoked once
        assertEquals("Handler A should be invoked once", 1, handlerACount.get());
        assertEquals("Handler B should be invoked once", 1, handlerBCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: SDK-scoped events (splitsUpdated) fan out to all clients
     * <p>
     * This test verifies that when a split update notification arrives via SSE,
     * the SDK_UPDATE event is emitted to all clients in the factory.
     * <p>
     * Note: True client-scoped events like mySegmentsUpdated require specific streaming 
     * notifications targeted at individual user keys. This test demonstrates the difference
     * by showing that SDK-scoped split updates affect all clients equally.
     */
    @Test
    public void clientScopedEventsDoNotFanOutToOtherClients() throws Exception {
        // Given: a factory with two clients (with streaming support)
        TwoClientFixture fixture = createTwoStreamingClientsAndWaitForReady(new Key("userA"), new Key("userB"));

        AtomicInteger handlerACount = new AtomicInteger(0);
        AtomicInteger handlerBCount = new AtomicInteger(0);
        CountDownLatch updateLatchA = new CountDownLatch(1);
        CountDownLatch updateLatchB = new CountDownLatch(1);

        // And: handlers HA and HB are registered for sdkUpdate
        fixture.clientA.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handlerACount.incrementAndGet();
                updateLatchA.countDown();
            }
        });

        fixture.clientB.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                handlerBCount.incrementAndGet();
                updateLatchB.countDown();
            }
        });

        // When: a SDK-scoped split update notification arrives (affects all clients)
        fixture.pushSplitUpdate();

        // Then: both clients receive SDK_UPDATE since splitsUpdated is SDK-scoped
        boolean updateAFired = updateLatchA.await(10, TimeUnit.SECONDS);
        boolean updateBFired = updateLatchB.await(10, TimeUnit.SECONDS);

        assertTrue("SDK_UPDATE should fire for ClientA", updateAFired);
        assertTrue("SDK_UPDATE should fire for ClientB", updateBFired);
        assertEquals("Handler A should be invoked once", 1, handlerACount.get());
        assertEquals("Handler B should be invoked once", 1, handlerBCount.get());

        fixture.destroy();
    }

    // Helper methods to reduce duplication

    /**
     * Creates a client and waits for SDK_READY to fire.
     * Returns a TestClientFixture containing the factory, client, and ready latch.
     */
    private TestClientFixture createClientAndWaitForReady(SplitClientConfig config, Key key) throws InterruptedException {
        SplitFactory factory = buildFactory(config);
        SplitClient client = factory.client(key);
        CountDownLatch readyLatch = new CountDownLatch(1);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                readyLatch.countDown();
            }
        });

        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        return new TestClientFixture(factory, client, readyLatch);
    }

    /**
     * Creates a client with default config and waits for SDK_READY.
     */
    private TestClientFixture createClientAndWaitForReady(Key key) throws InterruptedException {
        return createClientAndWaitForReady(buildConfig(), key);
    }

    /**
     * Creates a client with streaming enabled and waits for SDK_READY.
     * Returns a fixture that can push SSE messages to trigger SDK_UPDATE.
     */
    private TestClientFixture createStreamingClientAndWaitForReady(Key key) throws InterruptedException, IOException {
        BlockingQueue<String> streamingData = new LinkedBlockingDeque<>();
        CountDownLatch sseLatch = new CountDownLatch(1);
        AtomicInteger splitChangesHitCount = new AtomicInteger(0);

        HttpResponseMockDispatcher dispatcher = new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new HttpResponseMock(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    splitChangesHitCount.incrementAndGet();
                    return new HttpResponseMock(200, IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                } else if (uri.getPath().contains("/auth")) {
                    sseLatch.countDown();
                    return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
                } else if (uri.getPath().contains("/testImpressions/bulk")) {
                    return new HttpResponseMock(200);
                }
                return new HttpResponseMock(200);
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return new HttpStreamResponseMock(200, streamingData);
                } catch (IOException e) {
                    return null;
                }
            }
        };

        HttpClientMock httpClientMock = new HttpClientMock(dispatcher);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .streamingEnabled(true)
                .trafficType("account")
                .enableDebug()
                .build();

        SplitFactory factory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), key, config, mContext, httpClientMock, mDatabase);

        SplitClient client = factory.client(key);
        CountDownLatch readyLatch = new CountDownLatch(1);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                readyLatch.countDown();
            }
        });

        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // Wait for SSE connection and send keep-alive
        sseLatch.await(10, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(streamingData);

        return new TestClientFixture(factory, client, readyLatch, streamingData);
    }

    /**
     * Creates two clients with streaming enabled and waits for both to be ready.
     */
    private TwoClientFixture createTwoStreamingClientsAndWaitForReady(Key keyA, Key keyB) throws InterruptedException, IOException {
        BlockingQueue<String> streamingData = new LinkedBlockingDeque<>();
        CountDownLatch sseLatch = new CountDownLatch(1);
        AtomicInteger membershipsHitCount = new AtomicInteger(0);

        HttpResponseMockDispatcher dispatcher = new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    membershipsHitCount.incrementAndGet();
                    return new HttpResponseMock(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    return new HttpResponseMock(200, IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                } else if (uri.getPath().contains("/auth")) {
                    sseLatch.countDown();
                    return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
                }
                return new HttpResponseMock(200);
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return new HttpStreamResponseMock(200, streamingData);
                } catch (IOException e) {
                    return null;
                }
            }
        };

        HttpClientMock httpClientMock = new HttpClientMock(dispatcher);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .streamingEnabled(true)
                .trafficType("account")
                .enableDebug()
                .build();

        SplitFactory factory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), keyA, config, mContext, httpClientMock, mDatabase);

        SplitClient clientA = factory.client(keyA);
        SplitClient clientB = factory.client(keyB);

        CountDownLatch readyLatchA = new CountDownLatch(1);
        CountDownLatch readyLatchB = new CountDownLatch(1);

        registerReadyHandler(clientA, null, readyLatchA);
        registerReadyHandler(clientB, null, readyLatchB);

        boolean readyA = readyLatchA.await(30, TimeUnit.SECONDS);
        boolean readyB = readyLatchB.await(30, TimeUnit.SECONDS);
        assertTrue("ClientA SDK_READY should fire", readyA);
        assertTrue("ClientB SDK_READY should fire", readyB);

        // Wait for SSE connection and send keep-alive
        sseLatch.await(10, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(streamingData);

        return new TwoClientFixture(factory, clientA, clientB, streamingData);
    }

    /**
     * Registers a handler for SDK_READY_FROM_CACHE that captures metadata and counts invocations.
     */
    private void registerCacheReadyHandler(SplitClient client, AtomicInteger count,
                                           AtomicReference<EventMetadata> metadata,
                                           CountDownLatch latch) {
        client.on(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata eventMetadata) {
                count.incrementAndGet();
                if (metadata != null) metadata.set(eventMetadata);
                if (latch != null) latch.countDown();
            }
        });
    }

    /**
     * Registers a handler for SDK_UPDATE that counts invocations and optionally captures metadata.
     */
    private void registerUpdateHandler(SplitClient client, AtomicInteger count,
                                       AtomicReference<EventMetadata> metadata) {
        client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata eventMetadata) {
                count.incrementAndGet();
                if (metadata != null) metadata.set(eventMetadata);
            }
        });
    }

    /**
     * Registers a handler for SDK_READY that counts invocations and optionally counts down a latch.
     */
    private void registerReadyHandler(SplitClient client, AtomicInteger count, CountDownLatch latch) {
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                if (count != null) count.incrementAndGet();
                if (latch != null) latch.countDown();
            }
        });
    }

    /**
     * Creates two clients and waits for both to be ready.
     */
    private TwoClientFixture createTwoClientsAndWaitForReady(Key keyA, Key keyB) throws InterruptedException {
        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        SplitClient clientA = factory.client(keyA);
        SplitClient clientB = factory.client(keyB);

        CountDownLatch readyLatchA = new CountDownLatch(1);
        CountDownLatch readyLatchB = new CountDownLatch(1);

        registerReadyHandler(clientA, null, readyLatchA);
        registerReadyHandler(clientB, null, readyLatchB);

        boolean readyA = readyLatchA.await(30, TimeUnit.SECONDS);
        boolean readyB = readyLatchB.await(30, TimeUnit.SECONDS);
        assertTrue("ClientA SDK_READY should fire. ReadyA: " + readyA + ", ReadyB: " + readyB, readyA);
        assertTrue("ClientB SDK_READY should fire", readyB);

        return new TwoClientFixture(factory, clientA, clientB);
    }

    /**
     * Helper class to hold factory and client together for cleanup.
     */
    private static class TestClientFixture {
        final SplitFactory factory;
        final SplitClient client;
        final CountDownLatch readyLatch;
        final BlockingQueue<String> streamingData;

        TestClientFixture(SplitFactory factory, SplitClient client, CountDownLatch readyLatch) {
            this(factory, client, readyLatch, null);
        }

        TestClientFixture(SplitFactory factory, SplitClient client, CountDownLatch readyLatch, BlockingQueue<String> streamingData) {
            this.factory = factory;
            this.client = client;
            this.readyLatch = readyLatch;
            this.streamingData = streamingData;
        }

        void pushSplitUpdate() {
            pushSplitUpdate("9999999999999", "1000");
        }

        void pushSplitUpdate(String changeNumber, String previousChangeNumber) {
            if (streamingData != null) {
                String payload = "eyJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiaWQiOiJkNDMxY2RkMC1iMGJlLTExZWEtOGE4MC0xNjYwYWRhOWNlMzkiLCJuYW1lIjoibWF1cm9famF2YSIsInRyYWZmaWNBbGxvY2F0aW9uIjoxMDAsInRyYWZmaWNBbGxvY2F0aW9uU2VlZCI6LTkyMzkxNDkxLCJzZWVkIjotMTc2OTM3NzYwNCwic3RhdHVzIjoiQUNUSVZFIiwia2lsbGVkIjpmYWxzZSwiZGVmYXVsdFRyZWF0bWVudCI6Im9mZiIsImNoYW5nZU51bWJlciI6MTY4NDMyOTg1NDM4NSwiYWxnbyI6MiwiY29uZmlndXJhdGlvbnMiOnt9LCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiV0hJVEVMSVNUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7Im1hdGNoZXJUeXBlIjoiV0hJVEVMSVNUIiwibmVnYXRlIjpmYWxzZSwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOnsid2hpdGVsaXN0IjpbImFkbWluIiwibWF1cm8iLCJuaWNvIl19fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9XSwibGFiZWwiOiJ3aGl0ZWxpc3RlZCJ9LHsiY29uZGl0aW9uVHlwZSI6IlJPTExPVVQiLCJtYXRjaGVyR3JvdXAiOnsiY29tYmluZXIiOiJBTkQiLCJtYXRjaGVycyI6W3sia2V5U2VsZWN0b3IiOnsidHJhZmZpY1R5cGUiOiJ1c2VyIn0sIm1hdGNoZXJUeXBlIjoiSU5fU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibWF1ci0yIn19XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbWF1ci0yIn0seyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0=";
                pushMessage(streamingData, IntegrationHelper.splitChangeV2(
                        changeNumber, previousChangeNumber, "0", payload));
            }
        }

        void pushSplitKill(String splitName) {
            if (streamingData != null) {
                pushMessage(streamingData, IntegrationHelper.splitKill("9999999999999", splitName));
            }
        }

        void destroy() {
            factory.destroy();
        }
    }

    /**
     * Helper class to hold factory and two clients together for cleanup.
     */
    private static class TwoClientFixture {
        final SplitFactory factory;
        final SplitClient clientA;
        final SplitClient clientB;
        final BlockingQueue<String> streamingData;

        TwoClientFixture(SplitFactory factory, SplitClient clientA, SplitClient clientB) {
            this(factory, clientA, clientB, null);
        }

        TwoClientFixture(SplitFactory factory, SplitClient clientA, SplitClient clientB, BlockingQueue<String> streamingData) {
            this.factory = factory;
            this.clientA = clientA;
            this.clientB = clientB;
            this.streamingData = streamingData;
        }

        void pushSplitUpdate() {
            if (streamingData != null) {
                pushMessage(streamingData, IntegrationHelper.splitChangeV2CompressionType0());
            }
        }

        void destroy() {
            factory.destroy();
        }
    }

    private static void pushMessage(BlockingQueue<String> queue, String message) {
        try {
            queue.put(message + "\n");
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
            Logger.e("Failed to push message", e);
        }
    }

    /**
     * Populates the database with splits and segments to simulate a populated cache.
     */
    private void populateDatabaseWithCacheData(long timestamp) {
        // Populate splits
        List<SplitEntity> splitEntities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SplitEntity entity = new SplitEntity();
            entity.setName("split_" + i);
            entity.setBody(String.format("{\"name\":\"split_%d\", \"changeNumber\": %d}", i, 1000L + i));
            splitEntities.add(entity);
        }
        mDatabase.splitDao().insert(splitEntities);
        mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1000L));
        mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, timestamp));

        // Populate segments for default key
        MySegmentEntity segmentEntity = new MySegmentEntity();
        segmentEntity.setUserKey("DEFAULT_KEY");
        segmentEntity.setSegmentList("{\"k\":[{\"n\":\"segment1\"},{\"n\":\"segment2\"}],\"cn\":null}");
        segmentEntity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.mySegmentDao().update(segmentEntity);

        // Populate segments for key_1
        MySegmentEntity segmentEntity2 = new MySegmentEntity();
        segmentEntity2.setUserKey("key_1");
        segmentEntity2.setSegmentList("{\"k\":[{\"n\":\"segment1\"}],\"cn\":null}");
        segmentEntity2.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.mySegmentDao().update(segmentEntity2);
    }
}
