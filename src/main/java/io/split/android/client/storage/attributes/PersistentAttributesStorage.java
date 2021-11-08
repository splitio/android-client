package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface PersistentAttributesStorage {

    void set(@Nullable Map<String, Object> attributes);

    @NonNull
    Map<String, Object> getAll();

    void clear();
}
