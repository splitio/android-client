package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributesStorage {

    @Nullable Object get(String name);

    @NonNull
    Map<String, Object> getAll();

    void set(String name, @NonNull Object value);

    void set(@NonNull Map<String, Object> attributes);

    void clear();

    void destroy();

    void remove(String name);

    /**
     * Merges attributes read from persistent storage into memory, without overwriting values already
     * set by the SDK consumer. If the consumer cleared attributes before the load completed, the
     * persisted attributes are discarded.
     */
    void loadFromPersistence(@NonNull Map<String, Object> persistedAttributes);
}
