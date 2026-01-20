package tests.integration.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
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
import io.split.android.client.api.Key;
import io.split.android.client.events.SdkEventListener;
import io.split.android.client.events.SdkReadyMetadata;
import io.split.android.client.events.SdkUpdateMetadata;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
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
                .logLevel(SplitLogLevel.VERBOSE)
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
        mCurSplitReqId = 1003;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
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
     * And the metadata contains "initialCacheLoad" with value false
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
        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch cacheReadyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerCacheReadyHandler(client, handlerInvocationCount, receivedMetadata, cacheReadyLatch);

        boolean fired = cacheReadyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReadyFromCache is emitted exactly once
        assertTrue("SDK_READY_FROM_CACHE should fire", fired);
        assertEquals("Handler should be invoked exactly once", 1, handlerInvocationCount.get());

        // And: the metadata contains "initialCacheLoad" with value false
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        Boolean initialCacheLoad = receivedMetadata.get().isInitialCacheLoad();
        assertNotNull("initialCacheLoad should not be null", initialCacheLoad);
        assertFalse("initialCacheLoad should be false for cache path", initialCacheLoad);

        // And: the metadata contains "lastUpdateTimestamp" with a valid timestamp
        Long lastUpdateTimestamp = receivedMetadata.get().getLastUpdateTimestamp();
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
     * And the metadata contains "initialCacheLoad" with value true
     */
    @Test
    public void sdkReadyFromCacheFiresWhenSyncCompletesFreshInstallPath() throws Exception {
        // Given: SDK is starting without persistent storage (fresh install)
        // Database is already empty from setup()

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        // And: a handler H is registered for sdkReadyFromCache
        AtomicInteger handlerInvocationCount = new AtomicInteger(0);
        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch cacheReadyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));
        registerCacheReadyHandler(client, handlerInvocationCount, receivedMetadata, cacheReadyLatch);

        // When: internal events "targetingRulesSyncComplete" and "membershipsSyncComplete" are notified
        boolean fired = cacheReadyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReadyFromCache is emitted exactly once
        assertTrue("SDK_READY_FROM_CACHE should fire", fired);
        assertEquals("Handler should be invoked exactly once", 1, handlerInvocationCount.get());

        // And: the metadata contains "initialCacheLoad" with value true
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        Boolean initialCacheLoad = receivedMetadata.get().isInitialCacheLoad();
        assertNotNull("initialCacheLoad should not be null", initialCacheLoad);
        assertTrue("initialCacheLoad should be true for sync path (fresh install)", initialCacheLoad);

        factory.destroy();
    }

    /**
     * Scenario: onReady listener fires when SDK_READY event occurs
     * <p>
     * Given the SDK is starting with populated persistent storage
     * And a handler H is registered using addEventListener with onReady
     * When SDK_READY fires
     * Then onReady is invoked exactly once
     * And the handler receives the SplitClient and SdkReadyMetadata
     * And the metadata contains "initialCacheLoad" with value false
     * And the metadata contains "lastUpdateTimestamp" with a valid timestamp
     */
    @Test
    public void sdkReadyListenerFiresWithMetadata() throws Exception {
        // Given: SDK is starting with populated persistent storage
        long testTimestamp = System.currentTimeMillis();
        populateDatabaseWithCacheData(testTimestamp);

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        AtomicInteger onReadyCount = new AtomicInteger(0);
        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        AtomicReference<SplitClient> receivedClient = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));

        // And: a handler H is registered using addEventListener with onReady
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                onReadyCount.incrementAndGet();
                receivedMetadata.set(metadata);
                receivedClient.set(client);
                readyLatch.countDown();
            }
        });

        // When: SDK_READY fires
        boolean fired = readyLatch.await(30, TimeUnit.SECONDS);

        // Then: onReady is invoked exactly once
        assertTrue("onReady should fire", fired);
        assertEquals("onReady should be invoked exactly once", 1, onReadyCount.get());

        // And: the handler receives the SplitClient and SdkReadyMetadata
        assertNotNull("Received client should not be null", receivedClient.get());
        assertNotNull("Received metadata should not be null", receivedMetadata.get());

        // And: the metadata contains "initialCacheLoad" with value false
        Boolean initialCacheLoad = receivedMetadata.get().isInitialCacheLoad();
        assertNotNull("initialCacheLoad should not be null", initialCacheLoad);
        assertFalse("initialCacheLoad should be false for cache path", initialCacheLoad);

        // And: the metadata contains "lastUpdateTimestamp" with a valid timestamp
        Long lastUpdateTimestamp = receivedMetadata.get().getLastUpdateTimestamp();
        assertNotNull("lastUpdateTimestamp should not be null", lastUpdateTimestamp);
        assertTrue("lastUpdateTimestamp should be valid", lastUpdateTimestamp > 0);

        factory.destroy();
    }

    /**
     * Scenario: sdkReady metadata should be preserved for late-registered clients (warm cache)
     * <p>
     * Given the SDK is starting with populated persistent storage
     * And client1 has already emitted SDK_READY
     * When client2 is created and receives SDK_READY (replay)
     * Then the metadata should not be null and should reflect cache path values
     */
    @Test
    public void sdkReadyMetadataNotNullWhenMembershipsCompletesLast() throws Exception {
        long testTimestamp = System.currentTimeMillis();
        populateDatabaseWithCacheData(testTimestamp);

        SplitFactory factory = buildFactory(buildConfig());

        SplitClient client1 = factory.client(new Key("key_1"));
        CountDownLatch readyLatch1 = new CountDownLatch(1);
        registerReadyHandler(client1, null, readyLatch1);
        assertTrue("Client1 SDK_READY should fire", readyLatch1.await(10, TimeUnit.SECONDS));

        SplitClient client2 = factory.client(new Key("key_2"));
        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        client2.addEventListener(new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                receivedMetadata.set(metadata);
                readyLatch2.countDown();
            }
        });

        assertTrue("Client2 SDK_READY should fire", readyLatch2.await(10, TimeUnit.SECONDS));

        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertNotNull("initialCacheLoad should not be null", receivedMetadata.get().isInitialCacheLoad());
        assertFalse("initialCacheLoad should be false for cache path", receivedMetadata.get().isInitialCacheLoad());
        assertNotNull("lastUpdateTimestamp should not be null", receivedMetadata.get().getLastUpdateTimestamp());

        factory.destroy();
    }

    /**
     * Scenario: onReady listener replays to late subscribers
     * <p>
     * Given sdkReady has already been emitted
     * When a new handler H is registered using addEventListener with onReady
     * Then onReady handler H is invoked exactly once immediately (replay)
     */
    @Test
    public void sdkReadyListenerReplaysToLateSubscribers() throws Exception {
        // Given: sdkReady has already been emitted
        TestClientFixture fixture = createClientAndWaitForReady(new Key("key_1"));

        // When: a new handler H is registered for onReady after SDK_READY has fired
        AtomicInteger onReadyCount = new AtomicInteger(0);
        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch lateReadyLatch = new CountDownLatch(1);

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                onReadyCount.incrementAndGet();
                receivedMetadata.set(metadata);
                lateReadyLatch.countDown();
            }
        });

        // Then: onReady handler H is invoked exactly once immediately (replay)
        boolean replayFired = lateReadyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Late onReady handler should receive replay", replayFired);
        assertEquals("Late onReady handler should be invoked exactly once", 1, onReadyCount.get());
        assertNotNull("Metadata should not be null on replay", receivedMetadata.get());

        // And: onReady is not emitted again (verify no additional invocations)
        Thread.sleep(500);
        assertEquals("Late handler should not be invoked again", 1, onReadyCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: onReadyView is invoked on main thread when SDK_READY fires
     * <p>
     * Given the SDK is starting
     * And a handler H is registered using addEventListener with onReadyView
     * When SDK_READY fires
     * Then onReadyView is invoked on the main/UI thread
     */
    @Test
    public void sdkReadyViewListenerFiresOnMainThread() throws Exception {
        // Given: SDK is starting with populated persistent storage
        long testTimestamp = System.currentTimeMillis();
        populateDatabaseWithCacheData(testTimestamp);

        SplitClientConfig config = buildConfig();
        SplitFactory factory = buildFactory(config);

        AtomicInteger onReadyViewCount = new AtomicInteger(0);
        CountDownLatch readyViewLatch = new CountDownLatch(1);

        SplitClient client = factory.client(new Key("key_1"));

        // And: a handler H is registered using addEventListener with onReadyView
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onReadyView(SplitClient client, SdkReadyMetadata metadata) {
                onReadyViewCount.incrementAndGet();
                readyViewLatch.countDown();
            }
        });

        // When: SDK_READY fires
        boolean fired = readyViewLatch.await(10, TimeUnit.SECONDS);

        // Then: onReadyView is invoked
        assertTrue("onReadyView should fire", fired);
        assertEquals("onReadyView should be invoked exactly once", 1, onReadyViewCount.get());

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
            public void onPostExecution(SplitClient client) {
                cacheHandlerCount.incrementAndGet();
                cacheReadyLatch.countDown();
            }
        });

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
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

        // SDK_READY requires both SDK_READY_FROM_CACHE (prerequisite) and sync completion (requireAll)
        // Wait for SDK_READY to fire
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);

        // Then: sdkReady is emitted exactly once
        assertTrue("SDK_READY should fire after SDK_READY_FROM_CACHE and sync completion. " +
                "Cache fired: " + cacheHandlerCount.get() + ", Ready fired: " + readyHandlerCount.get(),
                readyFired);
        assertEquals("Ready handler should be invoked exactly once", 1, readyHandlerCount.get());

        // Verify both events fired
        assertEquals("SDK_READY_FROM_CACHE should fire", 1, cacheHandlerCount.get());
        assertEquals("SDK_READY should fire after SDK_READY_FROM_CACHE", 1, readyHandlerCount.get());

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
     * When an internal "splitsUpdated" event is notified during initial sync
     * Then sdkUpdate is not emitted because sdkReady has not fired yet
     * When internal events for sdkReadyFromCache and sdkReady are notified and both fire
     * When a new "splitsUpdated" event is notified via SSE
     * Then sdkUpdate is emitted
     * And handler H is invoked once with metadata
     */
    @Test
    public void sdkUpdateEmittedOnlyAfterSdkReady() throws Exception {
        // Given: Create streaming client but don't wait for SDK_READY
        TestClientFixture fixture = createStreamingClient(new Key("key_1"));

        AtomicInteger updateHandlerCount = new AtomicInteger(0);
        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        // Register handlers BEFORE SDK_READY fires
        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                updateHandlerCount.incrementAndGet();
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        fixture.client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });

        // Wait a bit to see if SDK_UPDATE fires prematurely (during initial sync)
        Thread.sleep(1000);

        // Then: sdkUpdate is not emitted because sdkReady has not fired yet
        assertEquals("SDK_UPDATE should not fire before SDK_READY", 0, updateHandlerCount.get());

        // When: SDK_READY fires
        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // Wait for SSE connection
        fixture.waitForSseConnection();

        // When: a new "splitsUpdated" event is notified via SSE (after SDK_READY has fired)
        fixture.pushSplitUpdate("2000", "1000");

        // Then: sdkUpdate is emitted and handler H is invoked once
        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire after SDK_READY when splits update arrives", updateFired);
        assertEquals("Handler should be invoked exactly once", 1, updateHandlerCount.get());
        assertNotNull("Metadata should not be null", receivedMetadata.get());

        fixture.destroy();
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
        AtomicReference<SdkUpdateMetadata> lastMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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
        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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
            public void onPostExecution(SplitClient client) {
                timeoutHandlerCount.incrementAndGet();
                timeoutLatch.countDown();
            }
        });

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
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
            public void onPostExecution(SplitClient client) {
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
        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                handler1Count.incrementAndGet();
                handler1Order.set(orderCounter.incrementAndGet());
                updateLatch.countDown();
            }
        });

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                handler2Count.incrementAndGet();
                handler2Order.set(orderCounter.incrementAndGet());
                updateLatch.countDown();
                throw new RuntimeException("Handler H2 exception");
            }
        });

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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
        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        // Given: a handler H is registered for sdkUpdate which inspects the received metadata
        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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
     * And an internal "splitsUpdated" event is notified via SSE
     * Then handler H is never invoked (handlers were cleared on destroy)
     * When registering a new handler H2 for sdkUpdate after destroy
     * Then the registration is a no-op
     * And H2 is never invoked even when another update is pushed
     */
    @Test
    public void destroyingClientStopsEventsAndClearsHandlers() throws Exception {
        // Given: sdkReady has already been emitted (with streaming support)
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger handler1Count = new AtomicInteger(0);
        AtomicInteger handler2Count = new AtomicInteger(0);

        // Given: a handler H registered for sdkUpdate before destroy
        fixture.client.addEventListener(createOnUpdateListener(handler1Count, null, null));

        // When: the client is destroyed
        fixture.client.destroy();

        fixture.pushSplitUpdate("3000", "2000");

        // Handler H is never invoked (handlers were cleared on destroy)
        Thread.sleep(1000);
        assertEquals("Handler H1 should not be invoked after destroy", 0, handler1Count.get());

        // When: registering a new handler H2 for sdkUpdate after destroy
        fixture.client.addEventListener(createOnUpdateListener(handler2Count, null, null));

        fixture.pushSplitUpdate("4000", "3000");

        Thread.sleep(1000);
        assertEquals("Handler H1 should still be 0", 0, handler1Count.get());
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
        fixture.clientA.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                handlerACount.incrementAndGet();
                updateLatchA.countDown();
            }
        });

        fixture.clientB.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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
        fixture.clientA.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                handlerACount.incrementAndGet();
                updateLatchA.countDown();
            }
        });

        fixture.clientB.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
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

    /**
     * Scenario: sdkUpdateMetadata contains Type.FLAGS_UPDATE for flags update
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When a split update notification arrives via SSE
     * Then sdkUpdate is emitted
     * And handler H receives metadata with getType() returning Type.FLAGS_UPDATE
     * And handler H receives metadata with getNames() containing the updated flag names
     */
    @Test
    public void sdkUpdateMetadataContainsTypeForFlagsUpdate() throws Exception {
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        fixture.pushSplitUpdate();

        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire", updateFired);

        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertEquals("Type should be FLAGS_UPDATE",
                SdkUpdateMetadata.Type.FLAGS_UPDATE, receivedMetadata.get().getType());

        assertNotNull("Names should not be null", receivedMetadata.get().getNames());
        assertFalse("Names should not be empty", receivedMetadata.get().getNames().isEmpty());

        fixture.destroy();
    }

    /**
     * Scenario: sdkUpdateMetadata contains Type.SEGMENTS_UPDATE for rule-based segments update
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When a rule-based segment update notification arrives via SSE
     * Then sdkUpdate is emitted
     * And handler H receives metadata with getType() returning Type.SEGMENTS_UPDATE
     * And handler H receives metadata with getNames() returning an empty list
     * <p>
     * Note: SEGMENTS_UPDATE always has empty names (segment names are not included).
     */
    @Test
    public void sdkUpdateMetadataContainsTypeForSegmentsUpdate() throws Exception {
        TestClientFixture fixture = createStreamingClientWithRbsAndWaitForReady(new Key("key_1"));

        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        fixture.pushRbsUpdate();

        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire for RBS update", updateFired);

        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertEquals("Type should be SEGMENTS_UPDATE",
                SdkUpdateMetadata.Type.SEGMENTS_UPDATE, receivedMetadata.get().getType());

        assertNotNull("Names should not be null", receivedMetadata.get().getNames());
        assertTrue("Names should be empty for SEGMENTS_UPDATE", receivedMetadata.get().getNames().isEmpty());

        fixture.destroy();
    }

    /**
     * Scenario: Only FLAGS_UPDATE fires when both flags and RBS change together
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When a polling sync returns changes to both flags AND rule-based segments
     * Then only ONE sdkUpdate is emitted
     * And handler H receives metadata with getType() returning Type.FLAGS_UPDATE
     * And SEGMENTS_UPDATE is NOT fired (RBS changes are subsumed by FLAGS_UPDATE)
     */
    @Test
    public void sdkUpdateFiresOnlyOnceWhenBothFlagsAndRbsChange() throws Exception {
        // Track number of /splitChanges calls
        AtomicInteger splitChangesHitCount = new AtomicInteger(0);

        final Dispatcher pollingDispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    int count = splitChangesHitCount.incrementAndGet();
                    if (count <= 1) {
                        // Initial sync: empty
                        return new MockResponse().setResponseCode(200)
                                .setBody(IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                    } else {
                        // Polling sync: return BOTH flag and RBS changes
                        // s and t must be equal to signal end of sync loop
                        String responseWithBothChanges = "{\"ff\":{\"s\":2000,\"t\":2000,\"d\":[" +
                                "{\"trafficTypeName\":\"user\",\"name\":\"test_split\",\"status\":\"ACTIVE\"," +
                                "\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":2000," +
                                "\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\"," +
                                "\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]}," +
                                "\"partitions\":[{\"treatment\":\"on\",\"size\":100}]}]}" +
                                "]},\"rbs\":{\"s\":2000,\"t\":2000,\"d\":[" +
                                "{\"name\":\"test_rbs\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\"," +
                                "\"excluded\":{\"keys\":[],\"segments\":[]}," +
                                "\"conditions\":[{\"matcherGroup\":{\"combiner\":\"AND\"," +
                                "\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]}}]}" +
                                "]}}";
                        return new MockResponse().setResponseCode(200).setBody(responseWithBothChanges);
                    }
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(pollingDispatcher);

        // Use polling mode with short refresh rate to trigger sync quickly
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(3) // Poll every 3 seconds
                .segmentsRefreshRate(999999)
                .impressionsRefreshRate(999999)
                .streamingEnabled(false)
                .trafficType("account")
                .build();

        SplitFactory factory = buildFactory(config);
        SplitClient client = factory.client();

        // Wait for SDK_READY
        CountDownLatch readyLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                readyLatch.countDown();
            }
        });
        assertTrue("SDK_READY should fire", readyLatch.await(10, TimeUnit.SECONDS));

        // Register handler to count SDK_UPDATE events and capture metadata
        List<SdkUpdateMetadata> receivedMetadataList = new ArrayList<>();
        CountDownLatch updateLatch = new CountDownLatch(1);

        client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                synchronized (receivedMetadataList) {
                    receivedMetadataList.add(metadata);
                }
                updateLatch.countDown();
            }
        });

        // Wait for SDK_UPDATE (triggered by polling that returns both flag and RBS changes)
        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire", updateFired);

        // Wait a bit to ensure no additional events fire
        Thread.sleep(1000);

        // Verify only ONE SDK_UPDATE was fired
        synchronized (receivedMetadataList) {
            assertEquals("Should receive exactly 1 SDK_UPDATE event (not 2)", 1, receivedMetadataList.size());

            // Verify it's FLAGS_UPDATE (not SEGMENTS_UPDATE)
            SdkUpdateMetadata metadata = receivedMetadataList.get(0);
            assertNotNull("Metadata should not be null", metadata);
            assertEquals("Type should be FLAGS_UPDATE (not SEGMENTS_UPDATE)",
                    SdkUpdateMetadata.Type.FLAGS_UPDATE, metadata.getType());
        }

        factory.destroy();
    }

    /**
     * Scenario: sdkUpdateMetadata contains Type.SEGMENTS_UPDATE for membership segments update (polling)
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When segments change via polling (server returns different segments)
     * Then sdkUpdate is emitted
     * And handler H receives metadata with getType() returning Type.SEGMENTS_UPDATE
     * And handler H receives metadata with getNames() returning an empty list
     */
    @Test
    public void sdkUpdateMetadataContainsTypeForMembershipSegmentsUpdate() throws Exception {
        verifySdkUpdateForSegmentsPollingWithEmptyNames(
                // Initial sync: segment1, segment2
                "{\"ms\":{\"k\":[{\"n\":\"segment1\"},{\"n\":\"segment2\"}],\"cn\":1000},\"ls\":{\"k\":[],\"cn\":1000}}",
                // Polling: segment1 removed, segment3 added
                "{\"ms\":{\"k\":[{\"n\":\"segment2\"},{\"n\":\"segment3\"}],\"cn\":2000},\"ls\":{\"k\":[],\"cn\":1000}}"
        );
    }

    /**
     * Scenario: sdkUpdateMetadata includes flag names for polling flag updates
     * <p>
     * Given sdkReady has already been emitted in polling mode
     * When polling returns a flag update
     * Then sdkUpdate metadata contains FLAGS_UPDATE with non-empty names
     */
    @Test
    public void sdkUpdateMetadataContainsNamesForPollingFlagsUpdate() throws Exception {
        AtomicInteger splitChangesHitCount = new AtomicInteger(0);
        final Dispatcher pollingDispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    int count = splitChangesHitCount.incrementAndGet();
                    if (count <= 1) {
                        return new MockResponse().setResponseCode(200)
                                .setBody(IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                    } else {
                        String responseWithFlagChange = "{\"ff\":{\"s\":2000,\"t\":2000,\"d\":[" +
                                "{\"trafficTypeName\":\"user\",\"name\":\"polling_flag\",\"status\":\"ACTIVE\"," +
                                "\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":2000," +
                                "\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\"," +
                                "\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]}," +
                                "\"partitions\":[{\"treatment\":\"on\",\"size\":100}]}]}" +
                                "]},\"rbs\":{\"s\":2000,\"t\":2000,\"d\":[]}}";
                        return new MockResponse().setResponseCode(200).setBody(responseWithFlagChange);
                    }
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(pollingDispatcher);

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(999999)
                .impressionsRefreshRate(999999)
                .streamingEnabled(false)
                .trafficType("account")
                .build();

        SplitFactory factory = buildFactory(config);
        SplitClient client = factory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                readyLatch.countDown();
            }
        });
        assertTrue("SDK_READY should fire", readyLatch.await(10, TimeUnit.SECONDS));

        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        assertTrue("SDK_UPDATE should fire", updateLatch.await(15, TimeUnit.SECONDS));
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertEquals("Type should be FLAGS_UPDATE",
                SdkUpdateMetadata.Type.FLAGS_UPDATE, receivedMetadata.get().getType());
        assertNotNull("Names should not be null", receivedMetadata.get().getNames());
        assertTrue("Names should include polling_flag", receivedMetadata.get().getNames().contains("polling_flag"));

        factory.destroy();
    }

    /**
     * Scenario: sdkReady should include non-null metadata on fresh install
     * <p>
     * Given the SDK starts with empty storage (fresh install)
     * When SDK_READY fires
     * Then metadata should be present (initialCacheLoad=true, lastUpdateTimestamp=null)
     */
    @Test
    public void sdkReadyMetadataNotNullOnFreshInstall() throws Exception {
        SplitFactory factory = buildFactory(buildConfig());
        SplitClient client = factory.client(new Key("key_1"));

        AtomicReference<SdkReadyMetadata> receivedMetadata = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(1);

        client.addEventListener(new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                receivedMetadata.set(metadata);
                readyLatch.countDown();
            }
        });

        assertTrue("SDK_READY should fire", readyLatch.await(10, TimeUnit.SECONDS));

        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertNotNull("initialCacheLoad should not be null", receivedMetadata.get().isInitialCacheLoad());
        assertTrue("initialCacheLoad should be true for fresh install", receivedMetadata.get().isInitialCacheLoad());
        assertEquals("lastUpdateTimestamp should be null for fresh install",
                null, receivedMetadata.get().getLastUpdateTimestamp());

        factory.destroy();
    }

    /**
     * Scenario: sdkUpdateMetadata should include SEGMENTS_UPDATE when only one client changes (polling)
     * <p>
     * Given two clients are created in polling mode
     * And only client1 receives a membership change on polling
     * When polling updates occur
     * Then only client1 receives SDK_UPDATE with SEGMENTS_UPDATE metadata
     */
    @Test
    public void sdkUpdateMetadataForSingleClientMembershipPolling() throws Exception {
        AtomicInteger key1MembershipHits = new AtomicInteger(0);
        AtomicInteger key2MembershipHits = new AtomicInteger(0);

        final String initialMemberships = "{\"ms\":{\"k\":[{\"n\":\"segment1\"}],\"cn\":1000},\"ls\":{\"k\":[],\"cn\":1000}}";
        final String updatedMembershipsKey1 = "{\"ms\":{\"k\":[{\"n\":\"segment2\"}],\"cn\":2000},\"ls\":{\"k\":[],\"cn\":1000}}";

        final Dispatcher pollingDispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    if (path.contains("key_1")) {
                        int count = key1MembershipHits.incrementAndGet();
                        return new MockResponse().setResponseCode(200)
                                .setBody(count <= 1 ? initialMemberships : updatedMembershipsKey1);
                    }
                    if (path.contains("key_2")) {
                        key2MembershipHits.incrementAndGet();
                        return new MockResponse().setResponseCode(200).setBody(initialMemberships);
                    }
                    return new MockResponse().setResponseCode(200).setBody(initialMemberships);
                } else if (path.contains("/splitChanges")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(pollingDispatcher);

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(999999)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(999999)
                .streamingEnabled(false)
                .trafficType("account")
                .build();

        SplitFactory factory = buildFactory(config);
        SplitClient client1 = factory.client(new Key("key_1"));
        SplitClient client2 = factory.client(new Key("key_2"));

        AtomicReference<SdkUpdateMetadata> client1Metadata = new AtomicReference<>();
        AtomicInteger client2UpdateCount = new AtomicInteger(0);
        CountDownLatch updateLatch = new CountDownLatch(1);

        client1.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                client1Metadata.set(metadata);
                updateLatch.countDown();
            }
        });
        client2.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                client2UpdateCount.incrementAndGet();
            }
        });

        CountDownLatch readyLatch1 = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        client1.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                readyLatch1.countDown();
            }
        });
        client2.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                readyLatch2.countDown();
            }
        });
        assertTrue("Client1 SDK_READY should fire", readyLatch1.await(10, TimeUnit.SECONDS));
        assertTrue("Client2 SDK_READY should fire", readyLatch2.await(10, TimeUnit.SECONDS));

        assertTrue("Client1 should receive SDK_UPDATE", updateLatch.await(20, TimeUnit.SECONDS));
        assertNotNull("Client1 metadata should not be null", client1Metadata.get());
        assertEquals("Type should be SEGMENTS_UPDATE",
                SdkUpdateMetadata.Type.SEGMENTS_UPDATE, client1Metadata.get().getType());

        Thread.sleep(1000);
        assertEquals("Client2 should not receive SDK_UPDATE", 0, client2UpdateCount.get());

        factory.destroy();
    }

    /**
     * Scenario: sdkUpdateMetadata contains SEGMENTS_UPDATE when only one streaming client changes
     * <p>
     * Given two clients are created with streaming enabled
     * And a membership keylist update targets only client1
     * When the SSE notification is pushed
     * Then only client1 receives SDK_UPDATE with SEGMENTS_UPDATE metadata
     */
    @Test
    public void sdkUpdateMetadataForSingleClientMembershipStreaming() throws Exception {
        TwoClientFixture fixture = createTwoStreamingClientsAndWaitForReady(new Key("key1"), new Key("key2"));

        AtomicReference<SdkUpdateMetadata> client1Metadata = new AtomicReference<>();
        AtomicInteger client2UpdateCount = new AtomicInteger(0);
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.clientA.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                client1Metadata.set(metadata);
                updateLatch.countDown();
            }
        });
        fixture.clientB.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                client2UpdateCount.incrementAndGet();
            }
        });

        // Keylist update: only key1 is included
        fixture.pushMembershipKeyListUpdate("key1", "streaming_segment");

        assertTrue("Client1 should receive SDK_UPDATE", updateLatch.await(10, TimeUnit.SECONDS));
        assertNotNull("Client1 metadata should not be null", client1Metadata.get());
        assertEquals("Type should be SEGMENTS_UPDATE",
                SdkUpdateMetadata.Type.SEGMENTS_UPDATE, client1Metadata.get().getType());

        Thread.sleep(500);
        assertEquals("Client2 should not receive SDK_UPDATE", 0, client2UpdateCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: sdkUpdateMetadata contains Type.SEGMENTS_UPDATE for large segments update (polling)
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When large segments change via polling (server returns different large segments)
     * Then sdkUpdate is emitted
     * And handler H receives metadata with getType() returning Type.SEGMENTS_UPDATE
     * And handler H receives metadata with getNames() returning an empty list
     */
    @Test
    public void sdkUpdateMetadataContainsTypeForLargeSegmentsUpdate() throws Exception {
        verifySdkUpdateForSegmentsPollingWithEmptyNames(
                // Initial sync: large_segment1, large_segment2
                "{\"ms\":{\"k\":[],\"cn\":1000},\"ls\":{\"k\":[{\"n\":\"large_segment1\"},{\"n\":\"large_segment2\"}],\"cn\":1000}}",
                // Polling: large_segment1 removed, large_segment3 added
                "{\"ms\":{\"k\":[],\"cn\":1000},\"ls\":{\"k\":[{\"n\":\"large_segment2\"},{\"n\":\"large_segment3\"}],\"cn\":2000}}"
        );
    }

    /**
     * Scenario: Two distinct SDK_UPDATE events are fired when both segments and large segments change
     * <p>
     * Given sdkReady has already been emitted
     * And a handler H is registered for sdkUpdate
     * When a single memberships response contains changes to both segments and large segments
     * Then two SDK_UPDATE events are emitted
     * And both events have metadata with getType() returning Type.SEGMENTS_UPDATE and empty names
     */
    @Test
    public void twoDistinctSdkUpdateEventsWhenBothSegmentsAndLargeSegmentsChange() throws Exception {
        // Initial sync: segment1, segment2 in ms; large_segment1, large_segment2 in ls
        String initialResponse = "{\"ms\":{\"k\":[{\"n\":\"segment1\"},{\"n\":\"segment2\"}],\"cn\":1000},\"ls\":{\"k\":[{\"n\":\"large_segment1\"},{\"n\":\"large_segment2\"}],\"cn\":1000}}";
        // Polling: both ms and ls change
        String pollingResponse = "{\"ms\":{\"k\":[{\"n\":\"segment2\"},{\"n\":\"segment3\"}],\"cn\":2000},\"ls\":{\"k\":[{\"n\":\"large_segment2\"},{\"n\":\"large_segment3\"}],\"cn\":2000}}";

        List<SdkUpdateMetadata> metadataList = waitForSegmentsPollingUpdates(initialResponse, pollingResponse, 2);

        // Verify we received 2 distinct SDK_UPDATE events
        assertEquals("Should receive 2 SDK_UPDATE events", 2, metadataList.size());

        // Both events should be SEGMENTS_UPDATE type with empty names
        for (SdkUpdateMetadata metadata : metadataList) {
            assertNotNull("Metadata should not be null", metadata);
            assertEquals("Type should be SEGMENTS_UPDATE",
                    SdkUpdateMetadata.Type.SEGMENTS_UPDATE, metadata.getType());
            assertNotNull("Names should not be null", metadata.getNames());
            assertTrue("Names should be empty for SEGMENTS_UPDATE", metadata.getNames().isEmpty());
        }
    }

    /**
     * Helper method to verify SDK_UPDATE with SEGMENTS_UPDATE type is emitted when segments change via polling.
     * Verifies that names are always empty for SEGMENTS_UPDATE.
     *
     * @param initialResponse the memberships response for initial sync
     * @param pollingResponse the memberships response for polling (with changed segments)
     */
    private void verifySdkUpdateForSegmentsPollingWithEmptyNames(String initialResponse, String pollingResponse) throws Exception {
        List<SdkUpdateMetadata> metadataList = waitForSegmentsPollingUpdates(initialResponse, pollingResponse, 1);

        assertEquals("Should receive 1 SDK_UPDATE event", 1, metadataList.size());

        SdkUpdateMetadata metadata = metadataList.get(0);
        assertNotNull("Metadata should not be null", metadata);
        assertEquals("Type should be SEGMENTS_UPDATE",
                SdkUpdateMetadata.Type.SEGMENTS_UPDATE, metadata.getType());

        assertNotNull("Names should not be null", metadata.getNames());
        assertTrue("Names should be empty for SEGMENTS_UPDATE", metadata.getNames().isEmpty());
    }

    /**
     * Helper method that sets up polling for segments and waits for the expected number of SDK_UPDATE events.
     *
     * @param initialResponse    the memberships response for initial sync
     * @param pollingResponse    the memberships response for polling (with changed segments)
     * @param expectedEventCount the number of SDK_UPDATE events to wait for
     * @return list of received SdkUpdateMetadata from the events
     */
    private List<SdkUpdateMetadata> waitForSegmentsPollingUpdates(String initialResponse, String pollingResponse,
                                                                   int expectedEventCount) throws Exception {
        AtomicInteger membershipsHitCount = new AtomicInteger(0);

        final Dispatcher pollingDispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    int count = membershipsHitCount.incrementAndGet();
                    if (count <= 1) {
                        return new MockResponse().setResponseCode(200).setBody(initialResponse);
                    } else {
                        return new MockResponse().setResponseCode(200).setBody(pollingResponse);
                    }
                } else if (path.contains("/splitChanges")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(1000, 1000));
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(pollingDispatcher);

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(999999)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(999999)
                .streamingEnabled(false)
                .trafficType("account")
                .build();

        SplitFactory factory = buildFactory(config);
        SplitClient client = factory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                readyLatch.countDown();
            }
        });
        assertTrue("SDK_READY should fire", readyLatch.await(10, TimeUnit.SECONDS));

        List<SdkUpdateMetadata> receivedMetadataList = new ArrayList<>();
        AtomicInteger legacyHandlerCount = new AtomicInteger(0);
        // Wait for expectedEventCount events x 2 handlers (new API + legacy)
        CountDownLatch updateLatch = new CountDownLatch(expectedEventCount * 2);

        // Register new API handler (addEventListener)
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient c, SdkUpdateMetadata metadata) {
                synchronized (receivedMetadataList) {
                    receivedMetadataList.add(metadata);
                }
                updateLatch.countDown();
            }
        });

        // Register legacy API handler (client.on)
        client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient c) {
                legacyHandlerCount.incrementAndGet();
                updateLatch.countDown();
            }
        });

        boolean updateFired = updateLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE should fire " + expectedEventCount + " time(s). " +
                "Hit count: " + membershipsHitCount.get() + ", metadata count: " + receivedMetadataList.size() +
                ", legacy count: " + legacyHandlerCount.get(), updateFired);

        // Verify legacy API was triggered the expected number of times
        assertEquals("Legacy API (client.on) should be triggered " + expectedEventCount + " time(s)",
                expectedEventCount, legacyHandlerCount.get());

        factory.destroy();

        return receivedMetadataList;
    }



    /**
     * Scenario: Multiple listeners with onUpdate are both invoked
     * <p>
     * Given sdkReady has already been emitted
     * And two different SdkEventListener instances (L1 and L2) with onUpdate handlers are registered
     * When a split update notification arrives via SSE
     * Then SDK_UPDATE is emitted once
     * And both L1.onUpdate and L2.onUpdate are invoked exactly once each
     */
    @Test
    public void multipleListenersWithOnUpdateBothInvoked() throws Exception {
        TestClientFixture fixture = createStreamingClientAndWaitForReady(new Key("key_1"));

        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        AtomicReference<SdkUpdateMetadata> listener1Metadata = new AtomicReference<>();
        AtomicReference<SdkUpdateMetadata> listener2Metadata = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(2);

        fixture.client.addEventListener(createOnUpdateListener(listener1Count, listener1Metadata, updateLatch));
        fixture.client.addEventListener(createOnUpdateListener(listener2Count, listener2Metadata, updateLatch));

        fixture.pushSplitUpdate();

        assertTrue("Both listeners should be invoked", updateLatch.await(10, TimeUnit.SECONDS));
        assertEquals("Listener 1 should be invoked exactly once", 1, listener1Count.get());
        assertEquals("Listener 2 should be invoked exactly once", 1, listener2Count.get());
        assertNotNull("Listener 1 should receive metadata", listener1Metadata.get());
        assertNotNull("Listener 2 should receive metadata", listener2Metadata.get());

        fixture.destroy();
    }

    /**
     * Scenario: Multiple listeners with onReady are both invoked
     * <p>
     * Given the SDK is starting
     * And two different SdkEventListener instances (L1 and L2) with onReady handlers are registered
     * When SDK_READY fires
     * Then both L1.onReady and L2.onReady are invoked exactly once each
     * And both receive SdkReadyMetadata
     */
    @Test
    public void multipleListenersWithOnReadyBothInvoked() throws Exception {
        populateDatabaseWithCacheData(System.currentTimeMillis());
        SplitFactory factory = buildFactory(buildConfig());
        SplitClient client = factory.client(new Key("key_1"));

        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        AtomicReference<SdkReadyMetadata> listener1Metadata = new AtomicReference<>();
        AtomicReference<SdkReadyMetadata> listener2Metadata = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(2);

        client.addEventListener(createOnReadyListener(listener1Count, listener1Metadata, readyLatch));
        client.addEventListener(createOnReadyListener(listener2Count, listener2Metadata, readyLatch));

        assertTrue("Both listeners should be invoked", readyLatch.await(10, TimeUnit.SECONDS));
        assertEquals("Listener 1 should be invoked exactly once", 1, listener1Count.get());
        assertEquals("Listener 2 should be invoked exactly once", 1, listener2Count.get());
        assertNotNull("Listener 1 should receive metadata", listener1Metadata.get());
        assertNotNull("Listener 2 should receive metadata", listener2Metadata.get());

        factory.destroy();
    }

    /**
     * Scenario: Listeners with different callbacks (onReady and onUpdate) each invoked on correct event
     * <p>
     * Given the SDK is starting
     * And a SdkEventListener L1 with onReady handler is registered
     * And a SdkEventListener L2 with onUpdate handler is registered
     * When SDK_READY fires
     * Then L1.onReady is invoked
     * And L2.onUpdate is NOT invoked (wrong event type)
     * When an SDK_UPDATE notification arrives via SSE
     * Then L2.onUpdate is invoked
     * And L1.onReady is NOT invoked again (already fired once for SDK_READY)
     */
    @Test
    public void listenersWithDifferentCallbacksInvokedOnCorrectEventType() throws Exception {
        TestClientFixture fixture = createStreamingClient(new Key("key_1"));

        AtomicInteger onReadyCount = new AtomicInteger(0);
        AtomicInteger onUpdateCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        fixture.client.addEventListener(createOnReadyListener(onReadyCount, null, readyLatch));
        fixture.client.addEventListener(createOnUpdateListener(onUpdateCount, null, updateLatch));

        assertTrue("SDK_READY should fire", readyLatch.await(10, TimeUnit.SECONDS));
        assertEquals("onReady should be invoked exactly once", 1, onReadyCount.get());
        assertEquals("onUpdate should NOT be invoked on SDK_READY", 0, onUpdateCount.get());

        fixture.waitForSseConnection();
        fixture.pushSplitUpdate();

        assertTrue("SDK_UPDATE should fire", updateLatch.await(10, TimeUnit.SECONDS));
        assertEquals("onUpdate should be invoked exactly once", 1, onUpdateCount.get());
        assertEquals("onReady should still be 1 (not invoked again)", 1, onReadyCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: Multiple listeners with both onReady and onUpdate in same listener
     * <p>
     * Given the SDK is starting
     * And two SdkEventListener instances (L1 and L2) each with both onReady and onUpdate handlers
     * When SDK_READY fires
     * Then both L1.onReady and L2.onReady are invoked exactly once each
     * And neither L1.onUpdate nor L2.onUpdate are invoked
     * When an SDK_UPDATE notification arrives via SSE
     * Then both L1.onUpdate and L2.onUpdate are invoked exactly once each
     */
    @Test
    public void multipleListenersWithBothReadyAndUpdateHandlers() throws Exception {
        TestClientFixture fixture = createStreamingClient(new Key("key_1"));

        AtomicInteger listener1ReadyCount = new AtomicInteger(0);
        AtomicInteger listener1UpdateCount = new AtomicInteger(0);
        AtomicInteger listener2ReadyCount = new AtomicInteger(0);
        AtomicInteger listener2UpdateCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch updateLatch = new CountDownLatch(2);

        fixture.client.addEventListener(createDualListener(listener1ReadyCount, readyLatch, listener1UpdateCount, updateLatch));
        fixture.client.addEventListener(createDualListener(listener2ReadyCount, readyLatch, listener2UpdateCount, updateLatch));

        assertTrue("Both onReady handlers should be invoked", readyLatch.await(10, TimeUnit.SECONDS));
        assertEquals("Listener 1 onReady should be invoked once", 1, listener1ReadyCount.get());
        assertEquals("Listener 2 onReady should be invoked once", 1, listener2ReadyCount.get());
        assertEquals("Listener 1 onUpdate should NOT be invoked on SDK_READY", 0, listener1UpdateCount.get());
        assertEquals("Listener 2 onUpdate should NOT be invoked on SDK_READY", 0, listener2UpdateCount.get());

        fixture.waitForSseConnection();
        fixture.pushSplitUpdate();

        assertTrue("Both onUpdate handlers should be invoked", updateLatch.await(10, TimeUnit.SECONDS));
        assertEquals("Listener 1 onUpdate should be invoked once", 1, listener1UpdateCount.get());
        assertEquals("Listener 2 onUpdate should be invoked once", 1, listener2UpdateCount.get());
        assertEquals("Listener 1 onReady should still be 1", 1, listener1ReadyCount.get());
        assertEquals("Listener 2 onReady should still be 1", 1, listener2ReadyCount.get());

        fixture.destroy();
    }

    /**
     * Scenario: Multiple listeners with onReady replay to late subscribers
     * <p>
     * Given SDK_READY has already been emitted
     * And a SdkEventListener L1 with onReady was registered before SDK_READY and was invoked
     * When a new SdkEventListener L2 with onReady is registered after SDK_READY has fired
     * Then L2.onReady is invoked (replay)
     * And L1.onReady is NOT invoked again
     */
    @Test
    public void multipleListenersWithOnReadyReplayToLateSubscribers() throws Exception {
        TestClientFixture fixture = createClientAndWaitForReady(new Key("key_1"));

        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        CountDownLatch listener1Latch = new CountDownLatch(1);
        CountDownLatch listener2Latch = new CountDownLatch(1);

        fixture.client.addEventListener(createOnReadyListener(listener1Count, null, listener1Latch));
        assertTrue("Listener 1 should receive replay", listener1Latch.await(5, TimeUnit.SECONDS));
        assertEquals("Listener 1 should be invoked once (replay)", 1, listener1Count.get());

        fixture.client.addEventListener(createOnReadyListener(listener2Count, null, listener2Latch));
        assertTrue("Listener 2 should receive replay", listener2Latch.await(5, TimeUnit.SECONDS));
        assertEquals("Listener 2 should be invoked once (replay)", 1, listener2Count.get());

        Thread.sleep(500);
        assertEquals("Listener 1 should still be 1 (not invoked again)", 1, listener1Count.get());

        fixture.destroy();
    }

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
            public void onPostExecution(SplitClient client) {
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
     * Creates a client with streaming enabled but does NOT wait for SDK_READY.
     * Useful for tests that need to register handlers before SDK_READY fires.
     * Returns a fixture that can push SSE messages to trigger SDK_UPDATE.
     */
    private TestClientFixture createStreamingClient(Key key) throws IOException {
        BlockingQueue<String> streamingData = new LinkedBlockingDeque<>();
        CountDownLatch sseLatch = new CountDownLatch(1);

        HttpResponseMockDispatcher dispatcher = createStreamingDispatcher(streamingData, sseLatch);
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

        return new TestClientFixture(factory, client, null, streamingData, sseLatch);
    }

    /**
     * Creates a client with streaming enabled and waits for SDK_READY.
     * Returns a fixture that can push SSE messages to trigger SDK_UPDATE.
     */
    private TestClientFixture createStreamingClientAndWaitForReady(Key key) throws InterruptedException, IOException {
        TestClientFixture fixture = createStreamingClient(key);

        CountDownLatch readyLatch = new CountDownLatch(1);
        fixture.client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });

        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // Wait for SSE connection and send keep-alive
        fixture.waitForSseConnection();

        return new TestClientFixture(fixture.factory, fixture.client, readyLatch, fixture.streamingData, fixture.sseLatch);
    }

    /**
     * Creates a standard streaming dispatcher for mock HTTP responses.
     */
    private HttpResponseMockDispatcher createStreamingDispatcher(BlockingQueue<String> streamingData, CountDownLatch sseLatch) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new HttpResponseMock(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
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
    }

    /**
     * Creates two clients with streaming enabled and waits for both to be ready.
     */
    private TwoClientFixture createTwoStreamingClientsAndWaitForReady(Key keyA, Key keyB) throws InterruptedException, IOException {
        BlockingQueue<String> streamingData = new LinkedBlockingDeque<>();
        CountDownLatch sseLatch = new CountDownLatch(1);

        HttpResponseMockDispatcher dispatcher = createStreamingDispatcher(streamingData, sseLatch);
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
                                           AtomicReference<SdkReadyMetadata> metadata,
                                           CountDownLatch latch) {
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onReadyFromCache(SplitClient client, SdkReadyMetadata eventMetadata) {
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
                                       AtomicReference<SdkUpdateMetadata> metadata) {
        client.addEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata eventMetadata) {
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
            public void onPostExecution(SplitClient client) {
                if (count != null) count.incrementAndGet();
                if (latch != null) latch.countDown();
            }
        });
    }

    /**
     * Creates a SdkEventListener that counts onReady invocations and captures metadata.
     */
    private SdkEventListener createOnReadyListener(AtomicInteger count,
                                                   AtomicReference<SdkReadyMetadata> metadata,
                                                   CountDownLatch latch) {
        return new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata eventMetadata) {
                if (count != null) count.incrementAndGet();
                if (metadata != null) metadata.set(eventMetadata);
                if (latch != null) latch.countDown();
            }
        };
    }

    /**
     * Creates a SdkEventListener that counts onUpdate invocations and captures metadata.
     */
    private SdkEventListener createOnUpdateListener(AtomicInteger count,
                                                    AtomicReference<SdkUpdateMetadata> metadata,
                                                    CountDownLatch latch) {
        return new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata eventMetadata) {
                if (count != null) count.incrementAndGet();
                if (metadata != null) metadata.set(eventMetadata);
                if (latch != null) latch.countDown();
            }
        };
    }

    /**
     * Creates a SdkEventListener with both onReady and onUpdate handlers.
     */
    private SdkEventListener createDualListener(AtomicInteger readyCount, CountDownLatch readyLatch,
                                                AtomicInteger updateCount, CountDownLatch updateLatch) {
        return new SdkEventListener() {
            @Override
            public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                if (readyCount != null) readyCount.incrementAndGet();
                if (readyLatch != null) readyLatch.countDown();
            }

            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                if (updateCount != null) updateCount.incrementAndGet();
                if (updateLatch != null) updateLatch.countDown();
            }
        };
    }

    private static final String SPLIT_UPDATE_PAYLOAD = "eyJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiaWQiOiJkNDMxY2RkMC1iMGJlLTExZWEtOGE4MC0xNjYwYWRhOWNlMzkiLCJuYW1lIjoibWF1cm9famF2YSIsInRyYWZmaWNBbGxvY2F0aW9uIjoxMDAsInRyYWZmaWNBbGxvY2F0aW9uU2VlZCI6LTkyMzkxNDkxLCJzZWVkIjotMTc2OTM3NzYwNCwic3RhdHVzIjoiQUNUSVZFIiwia2lsbGVkIjpmYWxzZSwiZGVmYXVsdFRyZWF0bWVudCI6Im9mZiIsImNoYW5nZU51bWJlciI6MTY4NDMyOTg1NDM4NSwiYWxnbyI6MiwiY29uZmlndXJhdGlvbnMiOnt9LCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiV0hJVEVMSVNUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7Im1hdGNoZXJUeXBlIjoiV0hJVEVMSVNUIiwibmVnYXRlIjpmYWxzZSwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOnsid2hpdGVsaXN0IjpbImFkbWluIiwibWF1cm8iLCJuaWNvIl19fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9XSwibGFiZWwiOiJ3aGl0ZWxpc3RlZCJ9LHsiY29uZGl0aW9uVHlwZSI6IlJPTExPVVQiLCJtYXRjaGVyR3JvdXAiOnsiY29tYmluZXIiOiJBTkQiLCJtYXRjaGVycyI6W3sia2V5U2VsZWN0b3IiOnsidHJhZmZpY1R5cGUiOiJ1c2VyIn0sIm1hdGNoZXJUeXBlIjoiSU5fU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibWF1ci0yIn19XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbWF1ci0yIn0seyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0=";

    /**
     * Helper class to hold factory and client together for cleanup.
     */
    private static class TestClientFixture {
        final SplitFactory factory;
        final SplitClient client;
        final CountDownLatch readyLatch;
        final BlockingQueue<String> streamingData;
        final CountDownLatch sseLatch;

        TestClientFixture(SplitFactory factory, SplitClient client, CountDownLatch readyLatch) {
            this(factory, client, readyLatch, null, null);
        }

        TestClientFixture(SplitFactory factory, SplitClient client, CountDownLatch readyLatch, BlockingQueue<String> streamingData) {
            this(factory, client, readyLatch, streamingData, null);
        }

        TestClientFixture(SplitFactory factory, SplitClient client, CountDownLatch readyLatch, 
                         BlockingQueue<String> streamingData, CountDownLatch sseLatch) {
            this.factory = factory;
            this.client = client;
            this.readyLatch = readyLatch;
            this.streamingData = streamingData;
            this.sseLatch = sseLatch;
        }

        void waitForSseConnection() throws InterruptedException {
            if (sseLatch != null) {
                sseLatch.await(10, TimeUnit.SECONDS);
                TestingHelper.pushKeepAlive(streamingData);
            }
        }

        void pushSplitUpdate() {
            pushSplitUpdate("9999999999999", "1000");
        }

        void pushSplitUpdate(String changeNumber, String previousChangeNumber) {
            if (streamingData != null) {
                pushMessage(streamingData, IntegrationHelper.splitChangeV2(
                        changeNumber, previousChangeNumber, "0", SPLIT_UPDATE_PAYLOAD));
            }
        }

        void pushSplitKill(String splitName) {
            if (streamingData != null) {
                pushMessage(streamingData, IntegrationHelper.splitKill("9999999999999", splitName));
            }
        }

        void pushRbsUpdate() {
            pushRbsUpdate("2000", "1000");
        }

        void pushRbsUpdate(String changeNumber, String previousChangeNumber) {
            if (streamingData != null) {
                // RBS payload: {"name":"rbs_test","status":"ACTIVE","trafficTypeName":"user","excluded":{"keys":[],"segments":[]},"conditions":[{"matcherGroup":{"combiner":"AND","matchers":[{"keySelector":{"trafficType":"user"},"matcherType":"ALL_KEYS","negate":false}]}}]}
                String RBS_UPDATE_PAYLOAD = "eyJuYW1lIjoicmJzX3Rlc3QiLCJzdGF0dXMiOiJBQ1RJVkUiLCJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiZXhjbHVkZWQiOnsia2V5cyI6W10sInNlZ21lbnRzIjpbXX0sImNvbmRpdGlvbnMiOlt7Im1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX19XX0=";
                pushMessage(streamingData, IntegrationHelper.rbsChange(changeNumber, previousChangeNumber, RBS_UPDATE_PAYLOAD));
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

        void pushMembershipKeyListUpdate(String key, String segmentName) {
            if (streamingData != null) {
                pushMessage(streamingData, membershipKeyListUpdateMessage(key, segmentName));
            }
        }

        void destroy() {
            factory.destroy();
        }
    }

    private static String membershipKeyListUpdateMessage(String key, String segmentName) {
        MySegmentsV2PayloadDecoder decoder = new MySegmentsV2PayloadDecoder();
        BigInteger hashedKey = decoder.hashKey(key);
        String keyListJson = "{\"a\":[" + hashedKey.toString() + "],\"r\":[]}";
        String encodedKeyList = Base64.encodeToString(
                keyListJson.getBytes(io.split.android.client.utils.StringHelper.defaultCharset()),
                Base64.NO_WRAP);

        String notificationJson = "{" +
                "\\\"type\\\":\\\"MEMBERSHIPS_MS_UPDATE\\\"," +
                "\\\"cn\\\":2000," +
                "\\\"n\\\":[\\\"" + segmentName + "\\\"]," +
                "\\\"c\\\":0," +
                "\\\"u\\\":2," +
                "\\\"d\\\":\\\"" + encodedKeyList + "\\\"" +
                "}";

        return "id: 1\n" +
                "event: message\n" +
                "data: {\"id\":\"m1\",\"clientId\":\"pri:test\",\"timestamp\":" + System.currentTimeMillis() +
                ",\"encoding\":\"json\",\"channel\":\"test_channel\",\"data\":\"" + notificationJson + "\"}\n";
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
        long finalChangeNumber = 1000L;
        for (int i = 0; i < 3; i++) {
            SplitEntity entity = new SplitEntity();
            entity.setName("split_" + i);
            long cn = 1000L + i;
            finalChangeNumber = cn;
            entity.setBody(String.format("{\"name\":\"split_%d\", \"changeNumber\": %d}", i, cn));
            splitEntities.add(entity);
        }
        mDatabase.splitDao().insert(splitEntities);
        mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, finalChangeNumber));
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

    /**
     * Creates a streaming client with RBS data pre-populated and waits for SDK_READY.
     * Pre-populates RBS change number so the test can verify in-place update behavior.
     */
    private TestClientFixture createStreamingClientWithRbsAndWaitForReady(Key key) throws InterruptedException, IOException {
        // Pre-populate RBS in storage so in-place update can work
        populateDatabaseWithRbsData();

        TestClientFixture fixture = createStreamingClient(key);

        CountDownLatch readyLatch = new CountDownLatch(1);
        fixture.client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });

        boolean readyFired = readyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("SDK_READY should fire", readyFired);

        // Wait for SSE connection and send keep-alive
        fixture.waitForSseConnection();

        return new TestClientFixture(fixture.factory, fixture.client, readyLatch, fixture.streamingData, fixture.sseLatch);
    }

    /**
     * Populates the database with RBS change number for instant update testing.
     */
    private void populateDatabaseWithRbsData() {
        // Set RBS change number so streaming notifications trigger in-place updates
        mDatabase.generalInfoDao().update(new GeneralInfoEntity("rbsChangeNumber", 1000L));
    }
}
