package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributesStorageImpl implements AttributesStorage {

    private final Map<String, Object> mInMemoryAttributes = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public Object get(String name) {
        return mInMemoryAttributes.get(name);
    }

    @NonNull
    @Override
    public Map<String, Object> getAll() {
        return new HashMap<>(mInMemoryAttributes);
    }

    @Override
    public void set(String name, @NonNull Object value) {
        mInMemoryAttributes.put(name, value);
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        mInMemoryAttributes.putAll(attributes);
    }

    @Override
    public void clear() {
        mInMemoryAttributes.clear();
    }

    @Override
    public void destroy() {
        mInMemoryAttributes.clear();
    }

    @Override
    public void remove(String key) {
        mInMemoryAttributes.remove(key);
    }

}
