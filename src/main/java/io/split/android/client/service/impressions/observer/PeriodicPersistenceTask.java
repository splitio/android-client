package io.split.android.client.service.impressions.observer;

import java.lang.ref.WeakReference;
import java.util.Map;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.utils.logger.Logger;

public class PeriodicPersistenceTask implements Runnable {

    private final Map<Long, Long> mCache;
    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final WeakReference<OnExecutedListener> mOnExecutedListener;

    PeriodicPersistenceTask(Map<Long, Long> cache, ImpressionsObserverCacheDao impressionsObserverCacheDao, OnExecutedListener onExecutedListener
    ) {
        mCache = cache;
        mImpressionsObserverCacheDao = impressionsObserverCacheDao;
        mOnExecutedListener = new WeakReference<>(onExecutedListener);
    }

    @Override
    public void run() {
        try {
            if (mCache != null) {
                for (Map.Entry<Long, Long> entry : mCache.entrySet()) {
                    try {
                        mImpressionsObserverCacheDao.insert(entry.getKey(), entry.getValue(), System.currentTimeMillis());
                    } catch (Exception ex) {
                        Logger.e("Error while persisting element in observer cache: " + ex.getLocalizedMessage());
                    }
                }
            }
        } catch (Exception ex) {
            Logger.e("Error while persisting observer cache: " + ex.getLocalizedMessage());
        } finally {
            if (mCache != null) {
                mCache.clear();
            }

            if (mOnExecutedListener.get() != null) {
                mOnExecutedListener.get().onExecuted();
            }
        }
    }

    interface OnExecutedListener {

        void onExecuted();
    }
}
