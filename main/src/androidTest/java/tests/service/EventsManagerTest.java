package tests.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.SplitEventExecutorResourcesMock;
import helper.TestingHelper;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
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
}
