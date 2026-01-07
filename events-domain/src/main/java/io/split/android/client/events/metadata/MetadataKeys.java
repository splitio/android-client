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
     * The type of update (FLAGS_UPDATE or SEGMENTS_UPDATE).
     */
    static final String TYPE = "type";

    /**
     * Names of entities that changed in this update.
     * <p>
     * For FLAGS_UPDATE, these are flag names.
     * For SEGMENTS_UPDATE, these are rule-based segment names.
     */
    static final String NAMES = "names";

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
