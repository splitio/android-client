package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.events.SdkReadyFromCacheMetadata;
import io.split.android.client.events.SdkUpdateMetadata;

/**
 * Converts {@link EventMetadata} to typed metadata objects for typed event tasks.
 * <p>
 * This class handles the conversion logic that was previously in the typed tasks.
 */
public class TypedTaskConverter {

    private TypedTaskConverter() {
        // Utility class
    }

    /**
     * Converts EventMetadata to SdkUpdateMetadata.
     *
     * @param metadata the event metadata, may be null
     * @return the typed metadata for SDK_UPDATE events
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static SdkUpdateMetadata convertForSdkUpdate(@Nullable EventMetadata metadata) {
        List<String> updatedFlags = null;
        if (metadata != null) {
            updatedFlags = (List<String>) metadata.get(MetadataKeys.UPDATED_FLAGS);
        }
        return new SdkUpdateMetadata(updatedFlags);
    }

    /**
     * Converts EventMetadata to SdkReadyFromCacheMetadata.
     *
     * @param metadata the event metadata, may be null
     * @return the typed metadata for SDK_READY_FROM_CACHE events
     */
    @NonNull
    public static SdkReadyFromCacheMetadata convertForSdkReadyFromCache(@Nullable EventMetadata metadata) {
        Boolean freshInstall = null;
        Long lastUpdateTimestamp = null;
        if (metadata != null) {
            freshInstall = (Boolean) metadata.get(MetadataKeys.FRESH_INSTALL);
            lastUpdateTimestamp = (Long) metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP);
        }
        return new SdkReadyFromCacheMetadata(freshInstall, lastUpdateTimestamp);
    }
}
