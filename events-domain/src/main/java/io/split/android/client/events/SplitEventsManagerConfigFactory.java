package io.split.android.client.events;

import java.util.HashSet;
import java.util.Set;

import io.harness.events.EventsManagerConfig;

/**
 * Factory for creating the {@link EventsManagerConfig} that defines the Split SDK event rules.
 * <p>
 * This configuration encapsulates the relationships between internal SDK events
 * and external client-facing events.
 */
final class SplitEventsManagerConfigFactory {

    private SplitEventsManagerConfigFactory() {
        // Utility class
    }

    /**
     * Creates the EventsManagerConfig for the Split SDK.
     * <p>
     * Event rules:
     * <ul>
     *   <li>SDK_READY: requires both splits and segments sync to complete, and SDK_READY_FROM_CACHE must fire first</li>
     *   <li>SDK_READY_FROM_CACHE: fires when EITHER all cache loading events complete OR all sync events complete</li>
     *   <li>SDK_READY_TIMED_OUT: fires when timeout is reached (suppressed if SDK_READY fired first)</li>
     *   <li>SDK_UPDATE: fires on any data update after SDK_READY</li>
     * </ul>
     *
     * @return the configured EventsManagerConfig
     */
    static EventsManagerConfig<SplitEvent, SplitInternalEvent> create() {
        // SDK_READY_FROM_CACHE fires when either:
        // 1. Cache path: All cache loading events complete (AND), OR
        // 2. Sync path: All sync events complete (AND)
        Set<SplitInternalEvent> cacheGroup = new HashSet<>();
        cacheGroup.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        cacheGroup.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        cacheGroup.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        cacheGroup.add(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE);

        Set<SplitInternalEvent> syncGroup = new HashSet<>();
        syncGroup.add(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);
        syncGroup.add(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);

        return EventsManagerConfig.<SplitEvent, SplitInternalEvent>builder()
                .requireAll(SplitEvent.SDK_READY,
                        SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE,
                        SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE)

                // SDK_READY_FROM_CACHE: OR of ANDs
                // Fires when (cache group all done) OR (sync group all done)
                .requireAny(SplitEvent.SDK_READY_FROM_CACHE, cacheGroup, syncGroup)

                .requireAny(SplitEvent.SDK_READY_TIMED_OUT,
                        SplitInternalEvent.SDK_READY_TIMEOUT_REACHED)

                .requireAny(SplitEvent.SDK_UPDATE,
                        SplitInternalEvent.SPLITS_UPDATED,
                        SplitInternalEvent.MY_SEGMENTS_UPDATED,
                        SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED,
                        SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED,
                        SplitInternalEvent.SPLIT_KILLED_NOTIFICATION)

                // SDK_READY requires SDK_READY_FROM_CACHE to fire first
                .prerequisite(SplitEvent.SDK_READY, SplitEvent.SDK_READY_FROM_CACHE)
                .prerequisite(SplitEvent.SDK_UPDATE, SplitEvent.SDK_READY)

                .suppressedBy(SplitEvent.SDK_READY_TIMED_OUT, SplitEvent.SDK_READY)

                .executionLimit(SplitEvent.SDK_READY, 1)
                .executionLimit(SplitEvent.SDK_READY_FROM_CACHE, 1)
                .executionLimit(SplitEvent.SDK_READY_TIMED_OUT, 1)
                .executionLimit(SplitEvent.SDK_UPDATE, -1) // unlimited

                // Metadata sources
                .metadataSource(SplitEvent.SDK_READY, SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE)
                // Cache path: if SDK_READY_FROM_CACHE fired because cache was loaded, use storage load metadata.
                .metadataSource(SplitEvent.SDK_READY_FROM_CACHE, cacheGroup,
                        SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE)
                // Sync path: if SDK_READY_FROM_CACHE fired alongside SDK_READY, use sync completion metadata.
                .metadataSource(SplitEvent.SDK_READY_FROM_CACHE, syncGroup,
                        SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE)

                .build();
    }
}
