package io.split.android.client.storage.cipher;

import androidx.core.util.Pools;

class ObjectPool<T> {
    private final Pools.SynchronizedPool<T> mPool;
    private final ObjectPoolFactory<T> mFactory;

    ObjectPool(int maxSize, ObjectPoolFactory<T> factory) {
        mFactory = factory;
        mPool = new Pools.SynchronizedPool<>(maxSize);
    }

    T acquire() {
        T instance = mPool.acquire();
        if (instance == null) {
            instance = mFactory.createObject();
        }
        return instance;
    }

    void release(T instance) {
        mPool.release(instance);
    }
}
