package io.split.android.client.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Typed metadata for SDK_UPDATE events.
 * <p>
 * Contains information about the type of update and the names of entities that were updated.
 */
public final class SdkUpdateMetadata {

    /**
     * The type of update that triggered the SDK_UPDATE event.
     */
    public enum Type {
        /**
         * Feature flags were updated.
         */
        FLAGS_UPDATE,

        /**
         * Rule-based segments were updated.
         * <p>
         */
        SEGMENTS_UPDATE
    }

    @Nullable
    private final Type mType;

    @NonNull
    private final List<String> mNames;

    /**
     * Creates a new SdkUpdateMetadata instance.
     *
     * @param type  the type of update, or null if not available
     * @param names the list of entity names that were updated, or null to use an empty list
     */
    public SdkUpdateMetadata(@Nullable Type type, @Nullable List<String> names) {
        mType = type;
        mNames = names != null ? names : Collections.emptyList();
    }

    /**
     * Returns the type of update that triggered this event.
     *
     * @return the update type, or null if not available
     */
    @Nullable
    public Type getType() {
        return mType;
    }

    /**
     * Returns the list of entity names that changed in this update.
     * <p>
     * For {@link Type#FLAGS_UPDATE}, this contains flag names.
     * For {@link Type#SEGMENTS_UPDATE}, this contains rule-based segment names.
     *
     * @return the list of updated entity names, never null (empty list if none)
     */
    @NonNull
    public List<String> getNames() {
        return mNames;
    }
}
