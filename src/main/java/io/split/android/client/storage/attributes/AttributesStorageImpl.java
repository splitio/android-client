package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.service.executor.SplitTaskExecutor;

public class AttributesStorageImpl implements AttributesStorage {

    @Nullable private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final SplitTaskExecutor splitTaskExecutor;
    private final Map<String, Object> mInMemoryAttributes = new ConcurrentHashMap<>();

    public AttributesStorageImpl(@Nullable PersistentAttributesStorage persistentAttributesStorage,
                                 @NonNull SplitTaskExecutor splitTaskExecutor) {
        mPersistentAttributesStorage = persistentAttributesStorage;
        this.splitTaskExecutor = splitTaskExecutor;
    }

    @Override
    public void loadLocal() {
        if (mPersistentAttributesStorage != null) {
            mInMemoryAttributes.clear();
            mInMemoryAttributes.putAll(mPersistentAttributesStorage.getAll());
        }
    }

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

        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        mInMemoryAttributes.putAll(attributes);

        if (mPersistentAttributesStorage != null) {
            mPersistentAttributesStorage.set(mInMemoryAttributes);
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
}
