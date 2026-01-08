package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

import java.util.ArrayList;
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
     * Creates metadata for SDK_UPDATE events when rule-based segments are updated.
     *
     * @param updatedSegmentNames the list of rule-based segment names that were updated
     * @return the event metadata with TYPE=SEGMENTS_UPDATE and NAMES containing the segment names
     */
    public static EventMetadata createUpdatedSegmentsMetadata(List<String> updatedSegmentNames) {
        return new EventMetadataBuilder()
                .put(MetadataKeys.TYPE, MetadataKeys.TYPE_SEGMENTS_UPDATE)
                .put(MetadataKeys.NAMES, new ArrayList<>(new HashSet<>(updatedSegmentNames)))
                .build();
    }

    /**
     * Creates metadata for the SDK_READY_FROM_CACHE event.
     *
     * @param lastUpdateTimestamp the timestamp when the cache was last updated, or null if not available
     * @param freshInstall        true if this is a fresh install (no prior cache), false if loaded from cache
     * @return the event metadata
     */
    public static EventMetadata createCacheReadyMetadata(@Nullable Long lastUpdateTimestamp, boolean freshInstall) {
        EventMetadataBuilder builder = new EventMetadataBuilder()
                .put(MetadataKeys.FRESH_INSTALL, freshInstall);

        if (lastUpdateTimestamp != null) {
            builder.put(MetadataKeys.LAST_UPDATE_TIMESTAMP, lastUpdateTimestamp);
        }

        return builder.build();
    }
}
