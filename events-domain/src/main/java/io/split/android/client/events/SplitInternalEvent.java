package io.split.android.client.events;

/**
 * Created by sarrubia on 4/6/18.
 */

public enum SplitInternalEvent {
    MY_SEGMENTS_LOADED_FROM_STORAGE,
    SPLITS_LOADED_FROM_STORAGE,
    MY_SEGMENTS_FETCHED,
    MY_SEGMENTS_UPDATED,
    SPLITS_FETCHED,
    SPLITS_UPDATED,
    SDK_READY_TIMEOUT_REACHED,
    SPLIT_KILLED_NOTIFICATION,
    ATTRIBUTES_LOADED_FROM_STORAGE,
    ENCRYPTION_MIGRATION_DONE,
    MY_LARGE_SEGMENTS_UPDATED,
    RULE_BASED_SEGMENTS_UPDATED,

    /**
     * Synthetic event: fired when splits sync completes (either SPLITS_FETCHED or SPLITS_UPDATED).
     * Used internally to simplify SDK_READY and SDK_READY_FROM_CACHE condition evaluation.
     */
    SPLITS_SYNC_COMPLETE,

    /**
     * Synthetic event: fired when segments sync completes (any of MY_SEGMENTS_FETCHED,
     * MY_SEGMENTS_UPDATED, or MY_LARGE_SEGMENTS_UPDATED).
     * Used internally to simplify SDK_READY and SDK_READY_FROM_CACHE condition evaluation.
     */
    SEGMENTS_SYNC_COMPLETE,
}
