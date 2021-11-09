package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AttributesStorageImpl implements AttributesStorage {

    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private boolean mCacheAttributes;
    private final Map<String, Object> mInMemoryAttributes = new HashMap<>();

    public AttributesStorageImpl(@NonNull PersistentAttributesStorage persistentAttributesStorage, boolean cacheAttributes) {
        mPersistentAttributesStorage = persistentAttributesStorage;
        mCacheAttributes = cacheAttributes;
    }

    @Override
    public void loadLocal() {
        overwriteValuesInMemory(mPersistentAttributesStorage.getAll());
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

        if (mCacheAttributes) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        overwriteValuesInMemory(attributes);

        if (mCacheAttributes) {
            mPersistentAttributesStorage.set(attributes);
        }
    }

    @Override
    public void clear() {
        mInMemoryAttributes.clear();
        mPersistentAttributesStorage.clear();
    }

    @Override
    public void setCacheEnabled(boolean enabled) {
        mCacheAttributes = enabled;
    }

    private void overwriteValuesInMemory(Map<String, Object> values) {
        mInMemoryAttributes.clear();
        mInMemoryAttributes.putAll(values);
    }
}
