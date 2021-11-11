package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributesStorage {

    void loadLocal();

    @Nullable Object get(String name);

    Map<String, Object> getAll();

    void set(String name, @NonNull Object value);

    void set(@NonNull Map<String, Object> attributes);

    void clear();

    void destroy();

    void remove(String name);
}
