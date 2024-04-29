package io.split.android.client.service.impressions.observer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.utils.logger.Logger;

public class PeriodicPersistenceTask implements Runnable {

    private final Map<Long, Long> mCache;
    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final AtomicBoolean mRunPeriodicSync;

    PeriodicPersistenceTask(Map<Long, Long> cache, ImpressionsObserverCacheDao impressionsObserverCacheDao, AtomicBoolean runPeriodicSync) {
        mCache = cache;
        mImpressionsObserverCacheDao = impressionsObserverCacheDao;
        mRunPeriodicSync = runPeriodicSync;
    }

    @Override
    public void run() {
        if (mRunPeriodicSync.get()) {
            try {
                for (Map.Entry<Long, Long> entry : mCache.entrySet()) {
                    try {
                        mImpressionsObserverCacheDao.insert(entry.getKey(), entry.getValue(), System.currentTimeMillis());
                    } catch (Exception ex) {
                        Logger.e("Error while persisting element in observer cache: " + ex.getLocalizedMessage());
                    }
                }
            } catch (Exception ex) {
                Logger.e("Error while persisting observer cache: " + ex.getLocalizedMessage());
            }
            mCache.clear();
            mRunPeriodicSync.compareAndSet(true, false);
        }
    }
}
