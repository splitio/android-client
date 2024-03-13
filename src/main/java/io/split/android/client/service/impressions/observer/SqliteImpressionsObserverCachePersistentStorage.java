package io.split.android.client.service.impressions.observer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

class SqliteImpressionsObserverCachePersistentStorage implements ImpressionsObserverCachePersistentStorage, ListenableLruCache.RemovalListener<Long> {

    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;

    public SqliteImpressionsObserverCachePersistentStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao) {
        mImpressionsObserverCacheDao = checkNotNull(impressionsObserverCacheDao);
    }

    @Override
    @WorkerThread
    public void insert(long hash, long time) {
        mImpressionsObserverCacheDao.insert(hash, time, System.currentTimeMillis());
    }

    @Override
    @Nullable
    @WorkerThread
    public Long get(long hash) {
        ImpressionsObserverCacheEntity entity = mImpressionsObserverCacheDao.get(hash);
        if (entity == null) {
            return null;
        }

        return entity.getTime();
    }

    @Override
    @WorkerThread
    public void deleteOutdated(long timestamp) {
        mImpressionsObserverCacheDao.deleteOldest(timestamp);
    }

    @Override
    public void onRemoval(Long key) {
        mImpressionsObserverCacheDao.delete(key);
    }
}
