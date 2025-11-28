package io.split.android.client.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Represents metadata associated with SDK events.
 * <p>
 * Values are sanitized to only allow String, Number, Boolean, or List&lt;String&gt;.
 */
public interface EventMetadata {

    /**
     * Returns the set of keys in this metadata.
     *
     * @return set of keys
     */
    @NonNull
    Set<String> keys();

    /**
     * Returns the collection of values in this metadata.
     *
     * @return collection of values
     */
    @NonNull
    Collection<Object> values();

    /**
     * Returns the value associated with the given key.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    @Nullable
    Object get(@NonNull String key);

    /**
     * Returns whether this metadata contains the given key.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(@NonNull String key);

    /**
     * Returns a copy of the underlying data as a Map.
     *
     * @return a copy of the metadata map
     */
    @NonNull
    Map<String, Object> toMap();
}

