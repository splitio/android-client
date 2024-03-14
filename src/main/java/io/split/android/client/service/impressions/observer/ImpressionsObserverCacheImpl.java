package io.split.android.client.service.impressions.observer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.split.android.client.utils.logger.Logger;

class ImpressionsObserverCacheImpl implements ImpressionsObserverCache {

    private final ImpressionsObserverCachePersistentStorage mPersistentStorage;
    private final ListenableLruCache<Long, Long> mCache;
    private final ReadWriteLock mLock;

    ImpressionsObserverCacheImpl(@NonNull ImpressionsObserverCachePersistentStorage persistentStorage,
                                 int cacheSize) {
        this(persistentStorage, new ListenableLruCache<>(cacheSize, persistentStorage));
    }

    @VisibleForTesting
    ImpressionsObserverCacheImpl(@NonNull ImpressionsObserverCachePersistentStorage persistentStorage,
                                 @NonNull ListenableLruCache<Long, Long> cache) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mCache = checkNotNull(cache);
        mLock = new ReentrantReadWriteLock();
    }

    @Nullable
    @Override
    public Long get(long hash) {
        // check in cache
        mLock.readLock().lock();
        try {
            Long cachedValue = getFromCache(hash);
            if (cachedValue != null) {
                return cachedValue;
            }
        } finally {
            mLock.readLock().unlock();
        }

        // otherwise, check in persistent storage
        mLock.writeLock().lock();
        try {
            // check in case another thread has already inserted the value
            Long cachedValue = getFromCache(hash);
            if (cachedValue != null) {
                return cachedValue;
            }

            Long persistedValue = getFromPersistentStorage(hash);
            if (persistedValue != null) {
                return persistedValue;
            }
        } finally {
            mLock.writeLock().unlock();
        }

        return null;
    }

    @Override
    public void put(long hash, long time) {
        mLock.writeLock().lock();
        try {
            putInCache(hash, time);
            putInPersistentStorage(hash, time);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Nullable
    private Long getFromCache(long hash) {
        try {
            Long cachedValue = mCache.get(hash);
            if (cachedValue != null) {
                return cachedValue;
            }
        } catch (Exception e) {
            logWarning("Error while getting value from cache", e);
        }
        return null;
    }

    @Nullable
    private Long getFromPersistentStorage(long hash) {
        try {
            Long persistedValue = mPersistentStorage.get(hash);
            if (persistedValue != null) {
                putInCache(hash, persistedValue);

                return persistedValue;
            }
        } catch (Exception e) {
            logWarning("Error while getting value from persistent storage", e);
        }
        return null;
    }

    private void putInCache(long hash, long time) {
        try {
            mCache.put(hash, time);
        } catch (Exception e) {
            logWarning("Error while putting value in cache", e);
        }
    }

    private void putInPersistentStorage(long hash, long time) {
        try {
            mPersistentStorage.insert(hash, time);
        } catch (Exception e) {
            logWarning("Error while putting value in persistent storage", e);
        }
    }

    private static void logWarning(String message, Exception e) {
        Logger.w("ImpressionsObserverCache: " + message + ": " + e.getLocalizedMessage());
    }
}
