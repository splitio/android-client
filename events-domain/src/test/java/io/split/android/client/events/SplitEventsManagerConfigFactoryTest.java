package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

import io.harness.events.EventsManagerConfig;

public class SplitEventsManagerConfigFactoryTest {

    @Test
    public void configIsNotNull() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();
        assertNotNull(config);
    }

    @Test
    public void sdkReadyRequiresTargetingRulesSyncCompleteAndMembershipsSyncComplete() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<SplitInternalEvent> requireAll = config.getRequireAll().get(SplitEvent.SDK_READY);
        assertNotNull("SDK_READY should have requireAll configuration", requireAll);
        assertTrue("SDK_READY should require TARGETING_RULES_SYNC_COMPLETE",
                requireAll.contains(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        assertTrue("SDK_READY should require MEMBERSHIPS_SYNC_COMPLETE",
                requireAll.contains(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE));
        assertEquals("SDK_READY should require exactly 2 events", 2, requireAll.size());
    }

    @Test
    public void sdkReadyHasPrerequisiteSdkReadyFromCache() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<SplitEvent> prerequisites = config.getPrerequisites().get(SplitEvent.SDK_READY);
        assertNotNull("SDK_READY should have prerequisites", prerequisites);
        assertTrue("SDK_READY should require SDK_READY_FROM_CACHE as prerequisite",
                prerequisites.contains(SplitEvent.SDK_READY_FROM_CACHE));
    }

    @Test
    public void sdkReadyHasExecutionLimitOne() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Integer limit = config.getExecutionLimits().get(SplitEvent.SDK_READY);
        assertNotNull("SDK_READY should have execution limit", limit);
        assertEquals("SDK_READY should fire at most once", 1, (int) limit);
    }

    @Test
    public void sdkReadyFromCacheHasOrOfAndsConfiguration() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<Set<SplitInternalEvent>> requireAnyGroups = config.getRequireAny().get(SplitEvent.SDK_READY_FROM_CACHE);
        assertNotNull("SDK_READY_FROM_CACHE should have requireAny configuration", requireAnyGroups);
        assertEquals("SDK_READY_FROM_CACHE should have 2 groups (cache and sync)", 2, requireAnyGroups.size());

        boolean hasCacheGroup = false;
        boolean hasSyncGroup = false;
        for (Set<SplitInternalEvent> group : requireAnyGroups) {
            if (group.size() == 4 &&
                    group.contains(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE) &&
                    group.contains(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE) &&
                    group.contains(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE) &&
                    group.contains(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE)) {
                hasCacheGroup = true;
            }
            if (group.size() == 2 &&
                    group.contains(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE) &&
                    group.contains(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE)) {
                hasSyncGroup = true;
            }
        }
        assertTrue("SDK_READY_FROM_CACHE should have cache group", hasCacheGroup);
        assertTrue("SDK_READY_FROM_CACHE should have sync group", hasSyncGroup);
    }

    @Test
    public void sdkReadyFromCacheHasExecutionLimitOne() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Integer limit = config.getExecutionLimits().get(SplitEvent.SDK_READY_FROM_CACHE);
        assertNotNull("SDK_READY_FROM_CACHE should have execution limit", limit);
        assertEquals("SDK_READY_FROM_CACHE should fire at most once", 1, (int) limit);
    }
    @Test
    public void sdkReadyTimedOutRequiresTimeoutReached() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<Set<SplitInternalEvent>> requireAnyGroups = config.getRequireAny().get(SplitEvent.SDK_READY_TIMED_OUT);
        assertNotNull("SDK_READY_TIMED_OUT should have requireAny configuration", requireAnyGroups);

        boolean hasTimeoutTrigger = false;
        for (Set<SplitInternalEvent> group : requireAnyGroups) {
            if (group.contains(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED)) {
                hasTimeoutTrigger = true;
                break;
            }
        }
        assertTrue("SDK_READY_TIMED_OUT should be triggered by SDK_READY_TIMEOUT_REACHED", hasTimeoutTrigger);
    }

    @Test
    public void sdkReadyTimedOutIsSuppressedBySdkReady() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<SplitEvent> suppressors = config.getSuppressedBy().get(SplitEvent.SDK_READY_TIMED_OUT);
        assertNotNull("SDK_READY_TIMED_OUT should have suppressors", suppressors);
        assertTrue("SDK_READY_TIMED_OUT should be suppressed by SDK_READY",
                suppressors.contains(SplitEvent.SDK_READY));
    }

    @Test
    public void sdkReadyTimedOutHasExecutionLimitOne() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Integer limit = config.getExecutionLimits().get(SplitEvent.SDK_READY_TIMED_OUT);
        assertNotNull("SDK_READY_TIMED_OUT should have execution limit", limit);
        assertEquals("SDK_READY_TIMED_OUT should fire at most once", 1, (int) limit);
    }

    @Test
    public void sdkUpdateHasCorrectTriggers() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<Set<SplitInternalEvent>> requireAnyGroups = config.getRequireAny().get(SplitEvent.SDK_UPDATE);
        assertNotNull("SDK_UPDATE should have requireAny configuration", requireAnyGroups);

        // Each trigger should be in its own singleton group
        boolean hasSplitsUpdated = false;
        boolean hasMySegmentsUpdated = false;
        boolean hasMyLargeSegmentsUpdated = false;
        boolean hasRuleBasedSegmentsUpdated = false;
        boolean hasSplitKilledNotification = false;

        for (Set<SplitInternalEvent> group : requireAnyGroups) {
            if (group.size() == 1) {
                if (group.contains(SplitInternalEvent.SPLITS_UPDATED)) hasSplitsUpdated = true;
                if (group.contains(SplitInternalEvent.MY_SEGMENTS_UPDATED)) hasMySegmentsUpdated = true;
                if (group.contains(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED)) hasMyLargeSegmentsUpdated = true;
                if (group.contains(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED)) hasRuleBasedSegmentsUpdated = true;
                if (group.contains(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION)) hasSplitKilledNotification = true;
            }
        }

        assertTrue("SDK_UPDATE should be triggered by SPLITS_UPDATED", hasSplitsUpdated);
        assertTrue("SDK_UPDATE should be triggered by MY_SEGMENTS_UPDATED", hasMySegmentsUpdated);
        assertTrue("SDK_UPDATE should be triggered by MY_LARGE_SEGMENTS_UPDATED", hasMyLargeSegmentsUpdated);
        assertTrue("SDK_UPDATE should be triggered by RULE_BASED_SEGMENTS_UPDATED", hasRuleBasedSegmentsUpdated);
        assertTrue("SDK_UPDATE should be triggered by SPLIT_KILLED_NOTIFICATION", hasSplitKilledNotification);
    }

    @Test
    public void sdkUpdateHasPrerequisiteSdkReady() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Set<SplitEvent> prerequisites = config.getPrerequisites().get(SplitEvent.SDK_UPDATE);
        assertNotNull("SDK_UPDATE should have prerequisites", prerequisites);
        assertTrue("SDK_UPDATE should require SDK_READY as prerequisite",
                prerequisites.contains(SplitEvent.SDK_READY));
    }

    @Test
    public void sdkUpdateHasUnlimitedExecutions() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        Integer limit = config.getExecutionLimits().get(SplitEvent.SDK_UPDATE);
        assertNotNull("SDK_UPDATE should have execution limit", limit);
        assertEquals("SDK_UPDATE should have unlimited executions (-1)", -1, (int) limit);
    }

    @Test
    public void evaluationOrderIsNotEmpty() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        assertNotNull("Evaluation order should not be null", config.getEvaluationOrder());
        assertFalse("Evaluation order should not be empty", config.getEvaluationOrder().isEmpty());
    }

    @Test
    public void evaluationOrderContainsAllConfiguredExternalEvents() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        assertTrue("Evaluation order should contain SDK_READY",
                config.getEvaluationOrder().contains(SplitEvent.SDK_READY));
        assertTrue("Evaluation order should contain SDK_READY_FROM_CACHE",
                config.getEvaluationOrder().contains(SplitEvent.SDK_READY_FROM_CACHE));
        assertTrue("Evaluation order should contain SDK_READY_TIMED_OUT",
                config.getEvaluationOrder().contains(SplitEvent.SDK_READY_TIMED_OUT));
        assertTrue("Evaluation order should contain SDK_UPDATE",
                config.getEvaluationOrder().contains(SplitEvent.SDK_UPDATE));
    }

    @Test
    public void evaluationOrderHasPrerequisitesBeforeDependents() {
        EventsManagerConfig<SplitEvent, SplitInternalEvent> config = SplitEventsManagerConfigFactory.create();

        // SDK_READY_FROM_CACHE must come before SDK_READY (prerequisite)
        int readyFromCacheIndex = config.getEvaluationOrder().indexOf(SplitEvent.SDK_READY_FROM_CACHE);
        int readyIndex = config.getEvaluationOrder().indexOf(SplitEvent.SDK_READY);
        assertTrue("SDK_READY_FROM_CACHE should be evaluated before SDK_READY",
                readyFromCacheIndex < readyIndex);

        // SDK_READY must come before SDK_UPDATE (prerequisite)
        int updateIndex = config.getEvaluationOrder().indexOf(SplitEvent.SDK_UPDATE);
        assertTrue("SDK_READY should be evaluated before SDK_UPDATE",
                readyIndex < updateIndex);
    }
}

