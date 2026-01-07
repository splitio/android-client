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

    public static EventMetadata createUpdatedFlagsMetadata(List<String> updatedSplitNames) {
        return new EventMetadataBuilder()
                .put(MetadataKeys.UPDATED_FLAGS, new ArrayList<>(new HashSet<>(updatedSplitNames)))
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
