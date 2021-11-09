package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributesStorage {

    void loadLocal();

    @Nullable Object get(String key);

    Map<String, Object> getAll();

    void set(String key, @Nullable Object value);

    void set(@NonNull Map<String, Object> attributes);

    void clear();

    void destroy();

    void remove(String key);
}
