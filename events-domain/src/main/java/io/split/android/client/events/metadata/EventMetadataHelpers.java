package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.SdkReadyFromCacheMetadataKeys;
import io.split.android.client.api.SdkUpdateMetadataKeys;

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
                .put(SdkUpdateMetadataKeys.UPDATED_FLAGS.name(), new ArrayList<>(new HashSet<>(updatedSplitNames)))
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
                .put(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL.name(), freshInstall);

        if (lastUpdateTimestamp != null) {
            builder.put(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP.name(), lastUpdateTimestamp);
        }

        return builder.build();
    }
}
