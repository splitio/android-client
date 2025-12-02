package io.split.android.client.events;

/**
 * Internal events used to track SDK initialization and data updates.
 */
public enum SplitInternalEvent {
    // Cache loading events
    MY_SEGMENTS_LOADED_FROM_STORAGE,
    SPLITS_LOADED_FROM_STORAGE,
    ATTRIBUTES_LOADED_FROM_STORAGE,
    ENCRYPTION_MIGRATION_DONE,

    // Data update events (fired only when data actually changed)
    MY_SEGMENTS_UPDATED,
    SPLITS_UPDATED,
    MY_LARGE_SEGMENTS_UPDATED,
    RULE_BASED_SEGMENTS_UPDATED,
    SPLIT_KILLED_NOTIFICATION,

    // Sync completion events (fired when sync completes, regardless of data change)
    TARGETING_RULES_SYNC_COMPLETE,
    MEMBERSHIPS_SYNC_COMPLETE,

    // Other events
    SDK_READY_TIMEOUT_REACHED,
}
