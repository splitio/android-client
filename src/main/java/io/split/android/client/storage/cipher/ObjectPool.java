package io.split.android.client.storage.cipher;

import androidx.core.util.Pools;

public class ObjectPool<T> {
    private final Pools.SynchronizedPool<T> mPool;
    private final ObjectPoolFactory<T> mFactory;

    public ObjectPool(int maxSize, ObjectPoolFactory<T> factory) {
        mFactory = factory;
        mPool = new Pools.SynchronizedPool<>(maxSize);
    }

    public T acquire() {
        T instance = mPool.acquire();
        if (instance == null) {
            instance = mFactory.createObject();
        }
        return instance;
    }

    public void release(T instance) {
        mPool.release(instance);
    }
}
