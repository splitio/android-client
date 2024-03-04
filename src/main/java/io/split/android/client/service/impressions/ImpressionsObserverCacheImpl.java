package io.split.android.client.service.impressions;

import android.os.SystemClock;
import android.util.Log;

import java.util.List;

import io.split.android.client.storage.db.ImpressionsObserverDao;
import io.split.android.client.storage.db.ImpressionsObserverEntity;

class ImpressionsObserverCacheImpl implements ImpressionsObserverCache {

    private volatile ListenableLruCache<Long, Long> mCache;
    private final ImpressionsObserverDao mImpressionsDedupeDao;
    private final int mSize;
    private final Object mInitializationLock = new Object();

    public ImpressionsObserverCacheImpl(int size, ImpressionsObserverDao impressionsDedupeDao) {
        long start = SystemClock.elapsedRealtime();
        mImpressionsDedupeDao = impressionsDedupeDao;
        mSize = size;
        Runnable cacheInitializer = new Runnable() {
            @Override
            public void run() {
                synchronized (mInitializationLock) {
                    mCache = new ListenableLruCache<>(size, new Listener(mImpressionsDedupeDao));
                    List<ImpressionsObserverEntity> persistedEntries = mImpressionsDedupeDao.getAll(size);
                    if (persistedEntries != null) {
                        for (ImpressionsObserverEntity entry : persistedEntries) {
                            if (entry != null) {
                                mCache.put(entry.getHash(), entry.getTime());
                            }
                        }
                    }
                    mInitializationLock.notifyAll();
                    Log.d("TestingTreatment", "initialization ready for " + persistedEntries.size());
                }
            }
        };
        new Thread(cacheInitializer).start();
        long end = SystemClock.elapsedRealtime();
        Log.d("TestingTreatment", "ImpressionsObserverCacheImpl: " + (end - start));
    }

    @Override
    public void put(Long hash, long time) {
        final long createdAt = System.currentTimeMillis();

        getCache().put(hash, time);

        mImpressionsDedupeDao.insert(hash, time, createdAt);
    }

    private ListenableLruCache<Long, Long> getCache() {
        while (mCache == null) {
            try {
                synchronized (mInitializationLock) {
                    mInitializationLock.wait(5000);

                    // If for some reason still null, create a new cache and unlock
                    if (mCache == null) {
                        mCache = new ListenableLruCache<>(mSize, new Listener(mImpressionsDedupeDao));
                        mInitializationLock.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                Log.d("TestingTreatment", "ImpressionsObserverCacheImpl: Error waiting for cache initialization", e);
            }
        }

        return mCache;
    }

    @Override
    public Long get(Long hash) {
        return getCache().get(hash);
    }

    private static class Listener implements RemovalListener<Long> {

        private final ImpressionsObserverDao mImpressionsDedupeDao;

        Listener(ImpressionsObserverDao impressionsDedupeDao) {
            mImpressionsDedupeDao = impressionsDedupeDao;
        }

        @Override
        public void onRemoval(Long key) {
            mImpressionsDedupeDao.delete(key);
        }
    }
}
