package io.split.android.client.service.impressions.observer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.utils.logger.Logger;

public class PeriodicPersistenceTask implements Runnable {

    private final Map<Long, Long> mCache;
    private final AtomicBoolean mRunPeriodicSync;
    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;

    public PeriodicPersistenceTask(Map<Long, Long> cache, ImpressionsObserverCacheDao impressionsObserverCacheDao) {
        mCache = cache;
        mRunPeriodicSync = new AtomicBoolean(false);
        mImpressionsObserverCacheDao = impressionsObserverCacheDao;
    }

    @Override
    public void run() {
        if (mRunPeriodicSync.get()) {
            try {
                for (Map.Entry<Long, Long> entry : mCache.entrySet()) {
                    try {
                        mImpressionsObserverCacheDao.insert(entry.getKey(), entry.getValue(), System.currentTimeMillis());
                    } catch (Exception ex) {
                        Logger.e("Error while persisting impression: " + ex.getLocalizedMessage());
                    }
                }
            } catch (Exception ex) {
                Logger.e("Error while persisting impressions: " + ex.getLocalizedMessage());
            }
            mCache.clear();
            mRunPeriodicSync.compareAndSet(true, false);
        }
    }
}
