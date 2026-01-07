package io.split.android.client.events;

import androidx.annotation.Nullable;

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
         * Note: This is for rule-based segments (RBS) ONLY, not for memberships
         * (my segments / large segments). Memberships have their own internal event
         * flow and don't emit SDK_UPDATE events with this metadata type.
         */
        SEGMENTS_UPDATE
    }

    @Nullable
    private final Type mType;

    @Nullable
    private final List<String> mNames;

    /**
     * Creates a new SdkUpdateMetadata instance.
     *
     * @param type  the type of update, or null if not available
     * @param names the list of entity names that were updated, or null if not available
     */
    public SdkUpdateMetadata(@Nullable Type type, @Nullable List<String> names) {
        mType = type;
        mNames = names;
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
     * @return the list of updated entity names, or null if not available
     */
    @Nullable
    public List<String> getNames() {
        return mNames;
    }
}
