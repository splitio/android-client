package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory attributes storage.
 *
 * <p>All state is guarded by a single lock, and every key touched by the SDK consumer is tracked as
 * "dirty". This allows an asynchronous load from persistent storage to complete without overwriting
 * newer values written by the consumer while the load was in flight (see
 * {@link #loadFromPersistence(Map)}).
 */
public class AttributesStorageImpl implements AttributesStorage {

    private final Object mLock = new Object();
    private final Map<String, Object> mInMemoryAttributes = new HashMap<>();
    /** Keys written or removed by the consumer; these must never be overwritten by a load. */
    private final Set<String> mDirtyKeys = new HashSet<>();
    /** Whether the consumer cleared all attributes; a pending load must then be discarded entirely. */
    private boolean mCleared = false;

    @Nullable
    @Override
    public Object get(String name) {
        synchronized (mLock) {
            return mInMemoryAttributes.get(name);
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getAll() {
        synchronized (mLock) {
            return new HashMap<>(mInMemoryAttributes);
        }
    }

    @Override
    public void set(String name, @NonNull Object value) {
        synchronized (mLock) {
            mInMemoryAttributes.put(name, value);
            mDirtyKeys.add(name);
            mCleared = false;
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        synchronized (mLock) {
            mInMemoryAttributes.putAll(attributes);
            mDirtyKeys.addAll(attributes.keySet());
            mCleared = false;
        }
    }

    @Override
    public void clear() {
        synchronized (mLock) {
            mDirtyKeys.addAll(mInMemoryAttributes.keySet());
            mInMemoryAttributes.clear();
            mCleared = true;
        }
    }

    @Override
    public void destroy() {
        synchronized (mLock) {
            mInMemoryAttributes.clear();
        }
    }

    @Override
    public void remove(String key) {
        synchronized (mLock) {
            mInMemoryAttributes.remove(key);
            mDirtyKeys.add(key);
            mCleared = false;
        }
    }

    @Override
    public void loadFromPersistence(@NonNull Map<String, Object> persistedAttributes) {
        synchronized (mLock) {
            if (mCleared) {
                return;
            }

            for (Map.Entry<String, Object> entry : persistedAttributes.entrySet()) {
                if (!mDirtyKeys.contains(entry.getKey())) {
                    mInMemoryAttributes.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
