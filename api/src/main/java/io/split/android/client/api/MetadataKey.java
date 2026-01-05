package io.split.android.client.api;

import androidx.annotation.NonNull;

/**
 * A typed metadata key used to retrieve values from {@link EventMetadata} without manual casting.
 * <p>
 * Instances are exposed as public constants grouped by event (e.g. {@link SdkUpdateMetadataKeys}).
 */
public final class MetadataKey<T> {

    private final String mName;

    public MetadataKey(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String name() {
        return mName;
    }
}


