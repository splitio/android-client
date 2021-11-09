package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributesStorage {

    void loadLocal();

    @Nullable Object get(String attributeName);

    Map<String, Object> getAll();

    void set(String key, @Nullable Object value);

    void set(@NonNull Map<String, Object> attributes);

    void clear();

    void setCacheEnabled(boolean enabled);
}
