package io.split.android.client.service.impressions.observer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.utils.logger.Logger;

public class PeriodicPersistenceTask implements Runnable {

    private final Map<Long, Long> mCache;
    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final WeakReference<OnExecutedListener> mOnExecutedListener;

    PeriodicPersistenceTask(Map<Long, Long> cache, ImpressionsObserverCacheDao impressionsObserverCacheDao, OnExecutedListener onExecutedListener) {
        mCache = cache;
        mImpressionsObserverCacheDao = impressionsObserverCacheDao;
        mOnExecutedListener = new WeakReference<>(onExecutedListener);
    }

    @Override
    public void run() {
        try {
            if (mCache != null) {
                try {
                    List<ImpressionsObserverCacheEntity> entities = new ArrayList<>();
                    for (Map.Entry<Long, Long> entry : mCache.entrySet()) {
                        try {
                            entities.add(new ImpressionsObserverCacheEntity(entry.getKey(), entry.getValue(), System.currentTimeMillis()));
                        } catch (Exception ex) {
                            Logger.e("Error while creating observer cache entity");
                        }
                    }

                    if (!entities.isEmpty()) {
                        mImpressionsObserverCacheDao.insert(entities);
                    }

                    mCache.clear();
                } catch (Exception ex) {
                    Logger.e("Error while persisting elements in observer cache: " + ex.getLocalizedMessage());
                }
            }
        } catch (Exception ex) {
            Logger.e("Error while persisting observer cache: " + ex.getLocalizedMessage());
        } finally {
            if (mOnExecutedListener.get() != null) {
                mOnExecutedListener.get().onExecuted();
            }
        }
    }

    interface OnExecutedListener {

        void onExecuted();
    }
}
