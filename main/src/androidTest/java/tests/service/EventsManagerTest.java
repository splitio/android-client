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
import io.split.android.client.events.SdkEventListener;
import io.split.android.client.events.SdkUpdateMetadata;
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
        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();

        // Wait for SDK_READY first
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        // Register for SDK_UPDATE with metadata callback using SdkEventListener
        eventManager.registerEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                receivedMetadata.set(metadata);
                updateLatch.countDown();
            }
        });

        // Make SDK_READY fire
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        Assert.assertTrue("SDK_READY should fire", readyLatch.await(5, TimeUnit.SECONDS));

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION, 
                EventMetadataHelpers.createUpdatedFlagsMetadata(Collections.singletonList("killed_flag")));

        Assert.assertTrue("SDK_UPDATE should fire", updateLatch.await(5, TimeUnit.SECONDS));
        Assert.assertNotNull("Metadata should not be null", receivedMetadata.get());
        List<String> names = receivedMetadata.get().getNames();
        Assert.assertNotNull("Names should not be null", names);
        Assert.assertTrue("Metadata should contain only killed_flag", names.size() == 1 && names.contains("killed_flag"));
        Assert.assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, receivedMetadata.get().getType());
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
    public void testSdkEventListenerReceivesMetadataOnCorrectThreads() throws InterruptedException {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(new SplitTaskExecutorImpl(), cfg.blockUntilReady());
        eventManager.setExecutionResources(new SplitEventExecutorResourcesMock());

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch allCalledLatch = new CountDownLatch(2); // Expect 2 calls (background and main thread)

        AtomicBoolean backgroundCalled = new AtomicBoolean(false);
        AtomicBoolean mainThreadCalled = new AtomicBoolean(false);

        AtomicBoolean backgroundOnMainThread = new AtomicBoolean(true); // Should be false
        AtomicBoolean mainThreadOnMainThread = new AtomicBoolean(false); // Should be true

        AtomicReference<SdkUpdateMetadata> backgroundMetadata = new AtomicReference<>();
        AtomicReference<SdkUpdateMetadata> mainThreadMetadata = new AtomicReference<>();

        // Wait for SDK_READY first
        eventManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        // Register SdkEventListener to receive typed metadata
        eventManager.registerEventListener(new SdkEventListener() {
            @Override
            public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
                backgroundCalled.set(true);
                backgroundOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                backgroundMetadata.set(metadata);
                allCalledLatch.countDown();
            }

            @Override
            public void onUpdateView(SplitClient client, SdkUpdateMetadata metadata) {
                mainThreadCalled.set(true);
                mainThreadOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                mainThreadMetadata.set(metadata);
                allCalledLatch.countDown();
            }
        });

        // Make SDK_READY fire
        eventManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        eventManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        Assert.assertTrue("SDK_READY should fire", readyLatch.await(5, TimeUnit.SECONDS));

        // Trigger SDK_UPDATE with metadata
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, 
                EventMetadataHelpers.createUpdatedFlagsMetadata(Arrays.asList("flag1", "flag2")));

        Assert.assertTrue("Both callbacks should be called", allCalledLatch.await(5, TimeUnit.SECONDS));

        Assert.assertTrue("Background method should be called", backgroundCalled.get());
        Assert.assertTrue("Main thread method should be called", mainThreadCalled.get());

        Assert.assertFalse("Background method should NOT run on main thread", backgroundOnMainThread.get());
        Assert.assertTrue("Main thread method SHOULD run on main thread", mainThreadOnMainThread.get());

        Assert.assertNotNull("Background metadata should not be null", backgroundMetadata.get());
        List<String> bgNames = backgroundMetadata.get().getNames();
        Assert.assertNotNull("Background names should not be null", bgNames);
        Assert.assertTrue("Background metadata should contain flag1", bgNames.contains("flag1"));
        Assert.assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, backgroundMetadata.get().getType());
        
        Assert.assertNotNull("Main thread metadata should not be null", mainThreadMetadata.get());
        List<String> mtNames = mainThreadMetadata.get().getNames();
        Assert.assertNotNull("Main thread names should not be null", mtNames);
        Assert.assertTrue("Main thread metadata should contain flag1", mtNames.contains("flag1"));
        Assert.assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, mainThreadMetadata.get().getType());
    }
}
