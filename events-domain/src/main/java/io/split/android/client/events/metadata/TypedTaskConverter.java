package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.events.SdkReadyMetadata;
import io.split.android.client.events.SdkUpdateMetadata;

/**
 * Converts {@link EventMetadata} to typed metadata objects for typed event tasks.
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
        SdkUpdateMetadata.Type type = null;
        List<String> names = null;

        if (metadata != null) {
            // Extract type
            String typeString = (String) metadata.get(MetadataKeys.TYPE);
            if (typeString != null) {
                try {
                    type = SdkUpdateMetadata.Type.valueOf(typeString);
                } catch (IllegalArgumentException ignored) {
                    // Unknown type, leave as null
                }
            }

            // Extract names
            names = (List<String>) metadata.get(MetadataKeys.NAMES);
        }

        return new SdkUpdateMetadata(type, names);
    }

    /**
     * Converts EventMetadata to SdkReadyMetadata.
     *
     * @param metadata the event metadata, may be null
     * @return the typed metadata for SDK_READY and SDK_READY_FROM_CACHE events
     */
    @NonNull
    public static SdkReadyMetadata convertForSdkReady(@Nullable EventMetadata metadata) {
        Boolean initialCacheLoad = null;
        Long lastUpdateTimestamp = null;
        if (metadata != null) {
            initialCacheLoad = (Boolean) metadata.get(MetadataKeys.INITIAL_CACHE_LOAD);
            lastUpdateTimestamp = (Long) metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP);
        }
        return new SdkReadyMetadata(initialCacheLoad, lastUpdateTimestamp);
    }
}
