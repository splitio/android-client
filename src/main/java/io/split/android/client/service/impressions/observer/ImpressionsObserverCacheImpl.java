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
            Long cachedValue = mCache.get(hash);
            if (cachedValue != null) {
                return cachedValue;
            }
        } catch (Exception e) {
            logError("Error while getting value from cache: ", e);
        } finally {
            mLock.readLock().unlock();
        }

        // otherwise, check in persistent storage
        mLock.writeLock().lock();
        try {
            // check in case another thread has already inserted the value
            Long cachedValue = mCache.get(hash);
            if (cachedValue != null) {
                return cachedValue;
            }

            Long persistedValue = mPersistentStorage.get(hash);
            if (persistedValue != null) {
                mCache.put(hash, persistedValue);

                return persistedValue;
            }
        } catch (Exception e) {
            logError("Error while getting value from persistent storage: ", e);
        } finally {
            mLock.writeLock().unlock();
        }

        return null;
    }

    @Override
    public void put(long hash, long time) {
        mLock.writeLock().lock();
        try {
            try {
                mCache.put(hash, time);
            } catch (Exception e) {
                logError("Error while putting value in cache: ", e);
            }
            try {
                mPersistentStorage.insert(hash, time);
            } catch (Exception e) {
                logError("Error while putting value in persistent storage: ", e);
            }
        } finally {
            mLock.writeLock().unlock();
        }
    }

    private static void logError(String message, Exception e) {
        Logger.w("ImpressionsObserverCache: " + message + e.getLocalizedMessage());
    }
}
