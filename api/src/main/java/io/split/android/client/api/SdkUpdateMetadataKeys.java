package io.split.android.client.api;

import java.util.List;

/**
 * Typed metadata keys for {@code SplitEvent.SDK_UPDATE} event metadata.
 */
public final class SdkUpdateMetadataKeys {
    private SdkUpdateMetadataKeys() {
        // no instances
    }

    /**
     * Names of flags that changed in this update.
     */
    public static final MetadataKey<List<String>> UPDATED_FLAGS = new MetadataKey<>("updatedFlags");
}
