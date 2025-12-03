package io.split.android.client.events.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.api.EventMetadata;

/**
 * Helper class for creating {@link EventMetadata} instances.
 * <p>
 * This keeps the metadata keys in a single place to avoid typos and inconsistencies.
 */
public class EventMetadataHelpers {

    private static final String KEY_UPDATED_FLAGS = "updatedFlags";

    private EventMetadataHelpers() {
        // Utility class
    }

    public static EventMetadata createUpdatedFlagsMetadata(List<String> updatedSplitNames) {
        return new EventMetadataBuilder()
                .put(KEY_UPDATED_FLAGS, new ArrayList<>(new HashSet<>(updatedSplitNames)))
                .build();
    }
}
