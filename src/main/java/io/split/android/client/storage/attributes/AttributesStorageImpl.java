package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributesStorageImpl implements AttributesStorage {

    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final boolean mIsPersistentCacheEnabled;
    private final Map<String, Object> mInMemoryAttributes = new ConcurrentHashMap<>();

    public AttributesStorageImpl(@NonNull PersistentAttributesStorage persistentAttributesStorage, boolean isPersistentCacheEnabled) {
        mPersistentAttributesStorage = persistentAttributesStorage;
        mIsPersistentCacheEnabled = isPersistentCacheEnabled;
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

        if (mIsPersistentCacheEnabled) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        overwriteValuesInMemory(attributes);

        if (mIsPersistentCacheEnabled) {
            mPersistentAttributesStorage.set(attributes);
        }
    }

    @Override
    public void clear() {
        mInMemoryAttributes.clear();
        mPersistentAttributesStorage.clear();
    }

    private void overwriteValuesInMemory(Map<String, Object> values) {
        mInMemoryAttributes.clear();
        mInMemoryAttributes.putAll(values);
    }
}
