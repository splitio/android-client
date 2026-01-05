package io.split.android.client.api;

/**
 * Typed metadata keys for {@code sdkReadyFromCache} event metadata.
 */
public final class SdkReadyFromCacheMetadataKeys {
    private SdkReadyFromCacheMetadataKeys() {
        // no instances
    }

    /**
     * True if this is a fresh install with no usable cache.
     */
    public static final MetadataKey<Boolean> FRESH_INSTALL = new MetadataKey<>("freshInstall");

    /**
     * Last successful cache timestamp in milliseconds since epoch.
     * <p>
     * May be absent when not available.
     */
    public static final MetadataKey<Long> LAST_UPDATE_TIMESTAMP = new MetadataKey<>("lastUpdateTimestamp");
}


