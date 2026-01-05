package io.split.android.client.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

/**
 * Represents metadata associated with SDK events.
 * <p>
 * Values are sanitized to only allow String, Number, Boolean, or List&lt;String&gt;.
 */
public interface EventMetadata {

    /**
     * Returns the number of entries in this metadata.
     */
    int size();

    /**
     * Returns whether this metadata has no entries.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the collection of values in this metadata.
     */
    @NonNull
    Collection<Object> values();

    /**
     * Returns the value associated with the given typed key.
     *
     * @param key the typed key to look up
     * @return the typed value associated with the key, or null if not found
     */
    @Nullable
    <T> T get(@NonNull MetadataKey<T> key);

    /**
     * Returns whether this metadata contains the given typed key.
     *
     * @param key the typed key to check
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(@NonNull MetadataKey<?> key);
}

