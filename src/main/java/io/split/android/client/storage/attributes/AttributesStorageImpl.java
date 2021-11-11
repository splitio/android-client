package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributesStorageImpl implements AttributesStorage {

    @Nullable private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final Map<String, Object> mInMemoryAttributes = new ConcurrentHashMap<>();

    public AttributesStorageImpl(@Nullable PersistentAttributesStorage persistentAttributesStorage) {
        mPersistentAttributesStorage = persistentAttributesStorage;
    }

    @Override
    public void loadLocal() {
        if (mPersistentAttributesStorage != null) {
            overwriteValuesInMemory(mPersistentAttributesStorage.getAll());
        }
    }

    @Nullable
    @Override
    public Object get(String name) {
        return mInMemoryAttributes.get(name);
    }

    @Override
    public Map<String, Object> getAll() {
        return new HashMap<>(mInMemoryAttributes);
    }

    @Override
    public void set(String name, @NonNull Object value) {
        mInMemoryAttributes.put(name, value);

        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        overwriteValuesInMemory(attributes);

        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.set(attributes);
        }
    }

    @Override
    public void clear() {
        mInMemoryAttributes.clear();
        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.clear();
        }
    }

    @Override
    public void destroy() {
        mInMemoryAttributes.clear();
    }

    @Override
    public void remove(String key) {
        mInMemoryAttributes.remove(key);

        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
        }
    }

    private void overwriteValuesInMemory(Map<String, Object> values) {
        mInMemoryAttributes.clear();
        mInMemoryAttributes.putAll(values);
    }
}
