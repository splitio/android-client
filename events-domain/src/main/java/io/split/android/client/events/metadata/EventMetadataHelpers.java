package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.api.EventMetadata;

/**
 * Helper class for creating {@link EventMetadata} instances.
 * <p>
 * Use these factory methods to create metadata for different event types.
 */
public class EventMetadataHelpers {

    private EventMetadataHelpers() {
        // Utility class
    }

    /**
     * Creates metadata for a FLAG_UPDATE event.
     * <p>
     * Flag names are deduplicated automatically.
     *
     * @param flagNames    the names of flags that were updated
     * @param changeNumber the changeNumber associated with this update, or null if not available
     * @return the event metadata
     */
    public static EventMetadata createFlagUpdateMetadata(
            List<String> flagNames,
            @Nullable Long changeNumber) {
        // Deduplicate flag names
        List<String> uniqueFlags = new ArrayList<>(new HashSet<>(flagNames));
        return new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                uniqueFlags,
                changeNumber
        );
    }

    /**
     * Creates metadata for a SEGMENT_UPDATE event.
     * <p>
     * Segment names are deduplicated automatically.
     *
     * @param segmentNames the names of segments that were updated
     * @param changeNumber the changeNumber associated with this update, or null if not available
     * @return the event metadata
     */
    public static EventMetadata createSegmentUpdateMetadata(
            List<String> segmentNames,
            @Nullable Long changeNumber) {
        // Deduplicate segment names
        List<String> uniqueSegments = new ArrayList<>(new HashSet<>(segmentNames));
        return new EventMetadataImpl(
                EventMetadata.Type.SEGMENT_UPDATE,
                uniqueSegments,
                changeNumber
        );
    }

    /**
     * Creates metadata for a FRESH_INSTALL event.
     * <p>
     * This is used when SDK_READY_FROM_CACHE fires but there was no prior cache
     * (fresh install scenario).
     *
     * @return the event metadata
     */
    public static EventMetadata createFreshInstallMetadata() {
        return new EventMetadataImpl(
                EventMetadata.Type.FRESH_INSTALL,
                Collections.emptyList(),
                null
        );
    }

    /**
     * Creates metadata for a FROM_CACHE event.
     * <p>
     * This is used when SDK_READY_FROM_CACHE fires after loading from existing cache.
     *
     * @param lastUpdateTimestamp the timestamp when the cache was last updated
     * @return the event metadata
     */
    public static EventMetadata createFromCacheMetadata(long lastUpdateTimestamp) {
        return new EventMetadataImpl(
                EventMetadata.Type.FROM_CACHE,
                Collections.emptyList(),
                lastUpdateTimestamp
        );
    }
}
