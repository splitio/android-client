package io.split.android.client.events;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Typed metadata for SDK_UPDATE events.
 * <p>
 * Contains information about flags that were updated in the event.
 */
public final class SdkUpdateMetadata {

    @Nullable
    private final List<String> mUpdatedFlags;

    /**
     * Creates a new SdkUpdateMetadata instance.
     *
     * @param updatedFlags the list of flag names that were updated, or null if not available
     */
    public SdkUpdateMetadata(@Nullable List<String> updatedFlags) {
        mUpdatedFlags = updatedFlags;
    }

    /**
     * Returns the list of flag names that changed in this update.
     *
     * @return the list of updated flag names, or null if not available
     */
    @Nullable
    public List<String> getUpdatedFlags() {
        return mUpdatedFlags;
    }
}

