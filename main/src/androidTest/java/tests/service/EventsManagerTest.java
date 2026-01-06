package tests.service;

import android.os.Looper;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import fake.SplitEventExecutorResourcesMock;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;

public class EventsManagerTest {
    @Test
    public void testSdkUpdateSplits() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // First make SDK_READY fire (prerequisite for SDK_UPDATE)
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        // Then trigger SDK_UPDATE
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(updateTask.onExecutedCalled);
    }

    @Test
    public void testSdkUpdateTriggersAfterReady() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // First make SDK_READY fire by completing sync
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        // Then trigger SDK_UPDATE with a data change
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(updateTask.onExecutedCalled);
    }

    @Test
    public void testSdkUpdateDoesNotTriggerBeforeReady() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // Fire UPDATED before SDK_READY - should NOT trigger SDK_UPDATE due to prerequisite
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        updateLatch.await(2, TimeUnit.SECONDS);

        Assert.assertFalse(updateTask.onExecutedCalled);
    }

    @Test
    public void testSdkUpdateSegments() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // First make SDK_READY fire (prerequisite for SDK_UPDATE)
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        // Then trigger SDK_UPDATE with segment change
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(updateTask.onExecutedCalled);
    }

    @Test
    public void testSdkUpdateTriggersOnSegmentChange() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // Make SDK_READY fire
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        // Then trigger SDK_UPDATE with a segment change
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(updateTask.onExecutedCalled);
    }

    @Test
    public void testSdkUpdateRequiresDataChange() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // Make SDK_READY fire with only SYNC_COMPLETE events (no UPDATED)
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        // No UPDATED events fired

        updateLatch.await(2, TimeUnit.SECONDS);

        // SDK_UPDATE should NOT fire because no data actually changed
        Assert.assertFalse(updateTask.onExecutedCalled);
    }

    @Test
    public void testKilledSplit() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch updateLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        // First make SDK_READY fire (prerequisite for SDK_UPDATE)
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        // Then trigger SDK_UPDATE with killed notification
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(updateTask.onExecutedCalled);
    }

    @Test
    public void testKilledSplitWithMetadata() throws InterruptedException {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        AtomicReference<EventMetadata> receivedMetadata = new AtomicReference<>();

        // Wait for SDK_READY first
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        // Register for SDK_UPDATE with metadata callback
        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        // Make SDK_READY fire
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        Assert.assertTrue("SDK_READY should fire", readyLatch.await(5, TimeUnit.SECONDS));

        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                Collections.singletonList("killed_flag"), null);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION, metadata);

        Assert.assertTrue("SDK_UPDATE should fire", updateLatch.await(5, TimeUnit.SECONDS));
        Assert.assertNotNull("Metadata should not be null", receivedMetadata.get());
        Assert.assertEquals("Metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, receivedMetadata.get().getType());
        List<String> metadataList = receivedMetadata.get().getValues();
        Assert.assertTrue("Metadata should contain only killed_flag", metadataList.size() == 1 && metadataList.contains("killed_flag"));
    }

    @Test
    public void testKilledSplitBeforeReady() throws InterruptedException {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());


        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(null);
        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);

        TestingHelper.delay(1000);

        Assert.assertFalse(updateTask.onExecutedCalled);
    }

    @Test
    public void testTimeoutSplitsUpdated() throws InterruptedException {

        SplitClientConfig cfg =  SplitClientConfig.builder().ready(2000).build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(null);
        TestingHelper.TestEventTask timeoutTask = TestingHelper.testTask(timeoutLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);
        eventManager.register(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);

        timeoutLatch.await(5, TimeUnit.SECONDS);

        Assert.assertFalse(updateTask.onExecutedCalled);
        Assert.assertTrue(timeoutTask.onExecutedCalled);
    }

    @Test
    public void testTimeoutMySegmentsUpdated() throws InterruptedException {

        SplitClientConfig cfg =  SplitClientConfig.builder().ready(2000).build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(null);
        TestingHelper.TestEventTask timeoutTask = TestingHelper.testTask(timeoutLatch);

        eventManager.register(SplitEvent.SDK_UPDATE, updateTask);
        eventManager.register(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);

        timeoutLatch.await(5, TimeUnit.SECONDS);

        Assert.assertFalse(updateTask.onExecutedCalled);
        Assert.assertTrue(timeoutTask.onExecutedCalled);
    }

    @Test
    public void testAllFourCallbackMethodsAreCalledWithCorrectThreadContext() throws InterruptedException {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch allCalledLatch = new CountDownLatch(4); // Expect 4 calls

        AtomicBoolean backgroundMetadataCalled = new AtomicBoolean(false);
        AtomicBoolean backgroundLegacyCalled = new AtomicBoolean(false);
        AtomicBoolean mainThreadMetadataCalled = new AtomicBoolean(false);
        AtomicBoolean mainThreadLegacyCalled = new AtomicBoolean(false);

        AtomicBoolean backgroundMetadataOnMainThread = new AtomicBoolean(true); // Should be false
        AtomicBoolean backgroundLegacyOnMainThread = new AtomicBoolean(true);   // Should be false
        AtomicBoolean mainThreadMetadataOnMainThread = new AtomicBoolean(false); // Should be true
        AtomicBoolean mainThreadLegacyOnMainThread = new AtomicBoolean(false);   // Should be true

        AtomicReference<EventMetadata> backgroundMetadata = new AtomicReference<>();
        AtomicReference<EventMetadata> mainThreadMetadata = new AtomicReference<>();

        // Wait for SDK_READY first
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        // Register a task that implements ALL FOUR methods
        eventManager.register(SplitEvent.SDK_UPDATE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                backgroundMetadataCalled.set(true);
                backgroundMetadataOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                backgroundMetadata.set(metadata);
                allCalledLatch.countDown();
            }

            @Override
            public void onPostExecution(SplitClient client) {
                backgroundLegacyCalled.set(true);
                backgroundLegacyOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                allCalledLatch.countDown();
            }

            @Override
            public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
                mainThreadMetadataCalled.set(true);
                mainThreadMetadataOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                mainThreadMetadata.set(metadata);
                allCalledLatch.countDown();
            }

            @Override
            public void onPostExecutionView(SplitClient client) {
                mainThreadLegacyCalled.set(true);
                mainThreadLegacyOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                allCalledLatch.countDown();
            }
        });

        // Make SDK_READY fire
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        Assert.assertTrue("SDK_READY should fire", readyLatch.await(5, TimeUnit.SECONDS));

        // Trigger SDK_UPDATE with metadata
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                Arrays.asList("flag1", "flag2"), null);
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);

        Assert.assertTrue("All four callbacks should be called", allCalledLatch.await(5, TimeUnit.SECONDS));

        Assert.assertTrue("Background metadata method should be called", backgroundMetadataCalled.get());
        Assert.assertTrue("Background legacy method should be called", backgroundLegacyCalled.get());
        Assert.assertTrue("Main thread metadata method should be called", mainThreadMetadataCalled.get());
        Assert.assertTrue("Main thread legacy method should be called", mainThreadLegacyCalled.get());

        Assert.assertFalse("Background metadata method should NOT run on main thread", backgroundMetadataOnMainThread.get());
        Assert.assertFalse("Background legacy method should NOT run on main thread", backgroundLegacyOnMainThread.get());
        Assert.assertTrue("Main thread metadata method SHOULD run on main thread", mainThreadMetadataOnMainThread.get());
        Assert.assertTrue("Main thread legacy method SHOULD run on main thread", mainThreadLegacyOnMainThread.get());

        Assert.assertNotNull("Background metadata should not be null", backgroundMetadata.get());
        Assert.assertEquals("Background metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, backgroundMetadata.get().getType());
        Assert.assertNotNull("Main thread metadata should not be null", mainThreadMetadata.get());
        Assert.assertEquals("Main thread metadata should be FLAG_UPDATE type", EventMetadata.Type.FLAG_UPDATE, mainThreadMetadata.get().getType());
    }
}
