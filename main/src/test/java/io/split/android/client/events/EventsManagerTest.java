package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.fake.SplitTaskExecutorStub;

public class EventsManagerTest {

    @Mock
    SplitEventExecutorResources resources;

    @Mock
    SplitClient client;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(resources.getSplitClient()).thenReturn(client);
    }

    @Test
    public void eventOnReady() {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), cfg.blockUntilReady());

        // Fire SYNC_COMPLETE events to trigger SDK_READY
        // This also triggers SDK_READY_FROM_CACHE via the sync path (OR-of-ANDs)
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY);

        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY));
        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE));
        assertFalse(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));
    }

    @Test
    public void eventOnReadyTimedOut() {
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), cfg.blockUntilReady());

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY_TIMED_OUT);

        assertFalse(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY));
        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));
    }

    @Test
    public void eventOnReadyAndOnReadyTimedOut() {
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), cfg.blockUntilReady());

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY_TIMED_OUT);

        //At this line timeout has been reached
        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));

        //But if after timeout event, the sync completes, SDK_READY should be triggered
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);

        shouldStop = false;
        maxExecutionTime = System.currentTimeMillis() + 10000;
        intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY);

        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY));
        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));
    }

    @Test
    public void eventOnReadyFromCacheSplitsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheMySegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheAttributesFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventEncryptionMigrationDoneFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheMyLargeSegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder()
                .build());
    }

    @Test
    public void sdkUpdateWithFeatureFlags() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.SPLITS_UPDATED, false);
    }

    @Test
    public void sdkUpdateWithMySegments() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.MY_SEGMENTS_UPDATED, false);
    }

    @Test
    public void sdkUpdateWithLargeSegmentsAndConfigEnabledEmitsEvent() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED, false);
    }

    @Test
    public void sdkUpdateWithLargeSegments() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED, false);
    }

    @Test
    public void sdkUpdateWithRuleBasedSegments() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED, false);
    }

    @Test
    public void sdkReadyWithSplitsAndUpdatedLargeSegments() {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), cfg.blockUntilReady());

        // Fire SYNC_COMPLETE events to trigger SDK_READY
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY);

        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY));
        assertFalse(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));
    }

    private static void sdkUpdateTest(SplitInternalEvent eventToCheck, boolean negate) throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), SplitClientConfig.builder()
                .build().blockUntilReady());

        CountDownLatch updateLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(1);
        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                updateLatch.countDown();
            }
        });
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        boolean readyAwait = readyLatch.await(3, TimeUnit.SECONDS);

        eventManager.notifyInternalEvent(eventToCheck);
        boolean updateAwait = updateLatch.await(3, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        if (!negate) {
            assertTrue(updateAwait);
        } else {
            assertFalse(updateAwait);
        }
    }

    private void eventOnReadyFromCache(List<SplitInternalEvent> eventList, SplitClientConfig config) {

        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), config.blockUntilReady());

        for (SplitInternalEvent event : eventList) {
            eventManager.notifyInternalEvent(event);
        }

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY_FROM_CACHE);

        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE));
    }

    private static void execute(boolean shouldStop, long intervalExecutionTime, long maxExecutionTime, SplitEventsManager eventManager, SplitEvent event) {
        while (!shouldStop) {
            try {
                Thread.sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(event)) {
                shouldStop = true;
            }
        }
    }

    @Test
    public void sdkUpdateWithMetadataCallsMetadataMethod() throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), 0);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();

        waitForSdkReady(eventManager, readyLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        EventMetadata metadata = createTestMetadata();
        triggerSdkUpdateWithMetadata(eventManager, metadata);

        boolean updateAwait = updateLatch.await(3, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE callback should be called", updateAwait);
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertEquals("Metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, receivedMetadata.get().getType());
    }

    @Test
    public void sdkUpdateWithMetadataCallsMetadataMethodOnMainThread() throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), 0);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();

        waitForSdkReady(eventManager, readyLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        EventMetadata metadata = createTestMetadata();
        triggerSdkUpdateWithMetadata(eventManager, metadata);

        boolean updateAwait = updateLatch.await(3, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE callback should be called on main thread", updateAwait);
        assertNotNull("Metadata should not be null", receivedMetadata.get());
        assertEquals("Metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, receivedMetadata.get().getType());
    }

    @Test
    public void sdkUpdateCallsLegacyMethodWhenOnlyLegacyImplemented() throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), 0);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        final boolean[] nonMetadataMethodCalled = {false};

        waitForSdkReady(eventManager, readyLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                nonMetadataMethodCalled[0] = true;
                updateLatch.countDown();
            }
        });

        EventMetadata metadata = createTestMetadata();
        triggerSdkUpdateWithMetadata(eventManager, metadata);

        boolean updateAwait = updateLatch.await(3, TimeUnit.SECONDS);
        assertTrue("SDK_UPDATE callback should be called", updateAwait);
        assertTrue("Legacy method should be called", nonMetadataMethodCalled[0]);
    }

    @Test
    public void sdkUpdateCallsBothMethodsWhenBothImplemented() throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), 0);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch bothCalledLatch = new CountDownLatch(2);
        final boolean[] metadataMethodCalled = {false};
        final boolean[] legacyMethodCalled = {false};
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();

        waitForSdkReady(eventManager, readyLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                metadataMethodCalled[0] = true;
                receivedMetadata.set(metadata);
                bothCalledLatch.countDown();
            }

            @Override
            public void onPostExecution(SplitClient client) {
                legacyMethodCalled[0] = true;
                bothCalledLatch.countDown();
            }
        });

        EventMetadata metadata = createTestMetadata();
        triggerSdkUpdateWithMetadata(eventManager, metadata);

        boolean bothCalled = bothCalledLatch.await(3, TimeUnit.SECONDS);
        assertTrue("Both callbacks should be called", bothCalled);
        assertTrue("Metadata method should be called", metadataMethodCalled[0]);
        assertTrue("Legacy method should also be called", legacyMethodCalled[0]);
        assertNotNull("Metadata should be passed to metadata method", receivedMetadata.get());
        assertEquals("Metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, receivedMetadata.get().getType());
    }

    @Test
    public void sdkReadyFromCacheCallsBothMethodsWhenBothImplemented() throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorStub(), 0);

        CountDownLatch bothCalledLatch = new CountDownLatch(2); // Expect 2 calls
        final boolean[] metadataMethodCalled = {false};
        final boolean[] legacyMethodCalled = {false};

        // Register a task that implements both versions
        eventManager.register(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                metadataMethodCalled[0] = true;
                bothCalledLatch.countDown();
            }

            @Override
            public void onPostExecution(SplitClient client) {
                legacyMethodCalled[0] = true;
                bothCalledLatch.countDown();
            }
        });

        // Trigger SDK_READY_FROM_CACHE
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventManager.notifyInternalEvent(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventManager.notifyInternalEvent(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);

        boolean bothCalled = bothCalledLatch.await(3, TimeUnit.SECONDS);
        assertTrue("Both callbacks should be called", bothCalled);
        assertTrue("Metadata method should be called", metadataMethodCalled[0]);
        assertTrue("Legacy method should also be called", legacyMethodCalled[0]);
    }

    private void waitForSdkReady(SplitEventsManager eventManager, CountDownLatch readyLatch) throws InterruptedException {
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        boolean readyAwait = readyLatch.await(3, TimeUnit.SECONDS);
        assertTrue("SDK_READY should be triggered", readyAwait);
    }

    private static EventMetadata createTestMetadata() {
        return EventMetadataHelpers.createFlagUpdateMetadata(
                Arrays.asList("flag1", "flag2"), null);
    }

    private static void triggerSdkUpdateWithMetadata(SplitEventsManager eventManager, EventMetadata metadata) {
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);
    }
}
