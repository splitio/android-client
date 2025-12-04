package io.split.android.client.events;

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
     *   <li>SDK_READY: requires both splits and segments sync to complete</li>
     *   <li>SDK_READY_FROM_CACHE: requires all cache loading events</li>
     *   <li>SDK_READY_TIMED_OUT: fires when timeout is reached (suppressed if SDK_READY fired first)</li>
     *   <li>SDK_UPDATE: fires on any data update after SDK_READY</li>
     * </ul>
     *
     * @return the configured EventsManagerConfig
     */
    static EventsManagerConfig<SplitEvent, SplitInternalEvent> create() {
        return EventsManagerConfig.<SplitEvent, SplitInternalEvent>builder()
                .requireAll(SplitEvent.SDK_READY,
                        SplitInternalEvent.SPLITS_SYNC_COMPLETE,
                        SplitInternalEvent.SEGMENTS_SYNC_COMPLETE)

                .requireAll(SplitEvent.SDK_READY_FROM_CACHE,
                        SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE,
                        SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE,
                        SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE,
                        SplitInternalEvent.ENCRYPTION_MIGRATION_DONE)

                .requireAny(SplitEvent.SDK_READY_TIMED_OUT,
                        SplitInternalEvent.SDK_READY_TIMEOUT_REACHED)

                .requireAny(SplitEvent.SDK_UPDATE,
                        SplitInternalEvent.SPLITS_UPDATED,
                        SplitInternalEvent.MY_SEGMENTS_UPDATED,
                        SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED,
                        SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED,
                        SplitInternalEvent.SPLIT_KILLED_NOTIFICATION)

                .prerequisite(SplitEvent.SDK_UPDATE, SplitEvent.SDK_READY)

                .suppressedBy(SplitEvent.SDK_READY_TIMED_OUT, SplitEvent.SDK_READY)

                .executionLimit(SplitEvent.SDK_READY, 1)
                .executionLimit(SplitEvent.SDK_READY_FROM_CACHE, 1)
                .executionLimit(SplitEvent.SDK_READY_TIMED_OUT, 1)
                .executionLimit(SplitEvent.SDK_UPDATE, -1) // unlimited

                .build();
    }
}
