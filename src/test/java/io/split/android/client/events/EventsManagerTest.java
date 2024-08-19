package io.split.android.client.events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorResources;
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
        SplitEventsManager eventManager = new SplitEventsManager(cfg, new SplitTaskExecutorStub());

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY);

        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY));
        assertFalse(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));
    }

    @Test
    public void eventOnReadyTimedOut() {
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg, new SplitTaskExecutorStub());

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
        SplitEventsManager eventManager = new SplitEventsManager(cfg, new SplitTaskExecutorStub());

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 200;

        execute(shouldStop, intervalExecutionTime, maxExecutionTime, eventManager, SplitEvent.SDK_READY_TIMED_OUT);

        //At this line timeout has been reached
        assertTrue(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT));

        //But if after timeout event, the Splits and MySegments are ready, SDK_READY should be triggered
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);

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
        eventList.add(SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheMySegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventList.add(SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheAttributesFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventList.add(SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventEncryptionMigrationDoneFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList, SplitClientConfig.builder().build());
    }

    @Test
    public void eventOnReadyFromCacheMyLargeSegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE);
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
    public void sdkUpdateWithLargeSegmentsAndConfigEnabledAndWaitForLargeSegmentsFalseEmitsEvent() throws InterruptedException {
        sdkUpdateTest(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED, false);
    }

    private static void sdkUpdateTest(SplitInternalEvent eventToCheck, boolean negate) throws InterruptedException {
        SplitEventsManager eventManager = new SplitEventsManager(SplitClientConfig.builder()
                .build(), new SplitTaskExecutorStub());

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

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_FETCHED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_FETCHED);
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

        SplitEventsManager eventManager = new SplitEventsManager(config, new SplitTaskExecutorStub());

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
}
