package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Helper class for creating {@link EventMetadata} instances.
 * <p>
 * This keeps the metadata keys in a single place to avoid typos and inconsistencies.
 */
public class EventMetadataHelpers {

    private EventMetadataHelpers() {
        // Utility class
    }

    /**
     * Creates metadata for SDK_UPDATE events when flags are updated.
     *
     * @param updatedFlagNames the list of flag names that were updated
     * @return the event metadata with TYPE=FLAGS_UPDATE and NAMES containing the flag names
     */
    public static EventMetadata createUpdatedFlagsMetadata(List<String> updatedFlagNames) {
        return new EventMetadataBuilder()
                .put(MetadataKeys.TYPE, MetadataKeys.TYPE_FLAGS_UPDATE)
                .put(MetadataKeys.NAMES, new ArrayList<>(new HashSet<>(updatedFlagNames)))
                .build();
    }

    /**
     * Creates metadata for SDK_UPDATE events when segments are updated.
     * <p>
     * SEGMENTS_UPDATE always has empty names - segment names are not included in the metadata.
     *
     * @return the event metadata with TYPE=SEGMENTS_UPDATE and empty NAMES list
     */
    public static EventMetadata createUpdatedSegmentsMetadata() {
        return new EventMetadataBuilder()
                .put(MetadataKeys.TYPE, MetadataKeys.TYPE_SEGMENTS_UPDATE)
                .put(MetadataKeys.NAMES, Collections.emptyList())
                .build();
    }

    /**
     * Creates metadata for the SDK_READY and SDK_READY_FROM_CACHE events.
     *
     * @param lastUpdateTimestamp the timestamp when the cache was last updated, or null if not available
     * @param initialCacheLoad    true if this is an initial cache load (no prior cache), false if loaded from cache
     * @return the event metadata
     */
    public static EventMetadata createReadyMetadata(@Nullable Long lastUpdateTimestamp, boolean initialCacheLoad) {
        EventMetadataBuilder builder = new EventMetadataBuilder()
                .put(MetadataKeys.INITIAL_CACHE_LOAD, initialCacheLoad);

        if (lastUpdateTimestamp != null) {
            builder.put(MetadataKeys.LAST_UPDATE_TIMESTAMP, lastUpdateTimestamp);
        }

        return builder.build();
    }

    /**
     * Creates metadata for TARGETING_RULES_SYNC_COMPLETE based on whether cache was already loaded.
     * <p>
     * If cache was already loaded (SDK_READY_FROM_CACHE fired), uses initialCacheLoad=false
     * and includes the update timestamp. Otherwise, uses initialCacheLoad=true with no timestamp.
     *
     * @param cacheAlreadyLoaded true if SDK_READY_FROM_CACHE has already fired
     * @param updateTimestamp    the timestamp from storage, used only if cacheAlreadyLoaded is true
     * @return the event metadata for sync complete
     */
    public static EventMetadata createSyncCompleteMetadata(boolean cacheAlreadyLoaded, @Nullable Long updateTimestamp) {
        Long timestamp = cacheAlreadyLoaded ? updateTimestamp : null;
        return createReadyMetadata(timestamp, !cacheAlreadyLoaded);
    }
}
