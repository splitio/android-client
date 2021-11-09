package io.split.android.client.storage.attributes;

import androidx.annotation.Nullable;

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
    public Object get(String attributeName) {
        return mInMemoryAttributes.get(attributeName);
    }

    @Override
    public Map<String, Object> getAll() {
        return mInMemoryAttributes;
    }

    @Override
    public void set(String key, @Nullable Object value) {
        mInMemoryAttributes.put(key, value);

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

    private void overwriteValuesInMemory(Map<String, Object> values) {
        mInMemoryAttributes.clear();
        mInMemoryAttributes.putAll(values);
    }
}
