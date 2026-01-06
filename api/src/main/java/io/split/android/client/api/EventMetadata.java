package io.split.android.client.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Represents metadata associated with SDK events.
 * <p>
 * This interface provides a unified way to access event-specific information
 * through a discriminated type system. The {@link Type} enum indicates what
 * kind of metadata this represents, and the appropriate getters can be used
 * to retrieve the relevant data.
 * <p>
 * <b>Usage patterns:</b>
 * <ul>
 *   <li>{@link Type#FLAG_UPDATE}: {@link #getValues()} returns flag names, {@link #getValue()} may return changeNumber</li>
 *   <li>{@link Type#SEGMENT_UPDATE}: {@link #getValues()} returns segment names, {@link #getValue()} may return changeNumber</li>
 *   <li>{@link Type#FRESH_INSTALL}: {@link #getValues()} returns empty list, {@link #getValue()} returns null</li>
 *   <li>{@link Type#FROM_CACHE}: {@link #getValues()} returns empty list, {@link #getValue()} returns lastUpdateTimestamp</li>
 * </ul>
 */
public interface EventMetadata {

    /**
     * Returns the type of metadata this represents.
     *
     * @return the metadata type, never null
     */
    @NonNull
    Type getType();

    /**
     * Returns the list of values associated with this metadata.
     * <p>
     * For {@link Type#FLAG_UPDATE}, this returns the names of flags that changed.
     * For {@link Type#SEGMENT_UPDATE}, this returns the names of segments that changed.
     * For other types, this returns an empty list.
     *
     * @return the list of values, never null (may be empty)
     */
    @NonNull
    List<String> getValues();

    /**
     * Returns the numeric value associated with this metadata.
     * <p>
     * For {@link Type#FLAG_UPDATE} and {@link Type#SEGMENT_UPDATE}, this may return a changeNumber.
     * For {@link Type#FROM_CACHE}, this returns the lastUpdateTimestamp.
     * For {@link Type#FRESH_INSTALL}, this returns null.
     *
     * @return the numeric value, or null if not applicable
     */
    @Nullable
    Long getValue();

    /**
     * The type of metadata this represents.
     */
    enum Type {
        /**
         * Flag definitions were updated.
         * <p>
         * {@link #getValues()} returns the names of flags that changed.
         * {@link #getValue()} may return the changeNumber if available.
         */
        FLAG_UPDATE,

        /**
         * Segment/RBS memberships were updated.
         * <p>
         * {@link #getValues()} returns the names of segments that changed.
         * {@link #getValue()} may return the changeNumber in future versions.
         */
        SEGMENT_UPDATE,

        /**
         * SDK_READY_FROM_CACHE with no prior cache (fresh install).
         * <p>
         * {@link #getValues()} returns an empty list.
         * {@link #getValue()} returns null.
         */
        FRESH_INSTALL,

        /**
         * SDK_READY_FROM_CACHE loaded from existing cache.
         * <p>
         * {@link #getValues()} returns an empty list.
         * {@link #getValue()} returns the lastUpdateTimestamp.
         */
        FROM_CACHE
    }
}
