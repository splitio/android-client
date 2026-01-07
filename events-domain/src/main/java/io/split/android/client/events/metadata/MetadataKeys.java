package io.split.android.client.events.metadata;

/**
 * Consolidated metadata keys for SDK events.
 * <p>
 * Package-private - for internal SDK use only.
 */
final class MetadataKeys {

    private MetadataKeys() {
        // no instances
    }

    // SDK_UPDATE event keys

    /**
     * Names of flags that changed in this update.
     */
    static final String UPDATED_FLAGS = "updatedFlags";

    // SDK_READY_FROM_CACHE event keys

    /**
     * True if this is a fresh install with no usable cache.
     */
    static final String FRESH_INSTALL = "freshInstall";

    /**
     * Last successful cache timestamp in milliseconds since epoch.
     * <p>
     * May be absent when not available.
     */
    static final String LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
}
