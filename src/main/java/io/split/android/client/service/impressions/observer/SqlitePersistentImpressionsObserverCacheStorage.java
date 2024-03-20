package io.split.android.client.service.impressions.observer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.concurrent.TimeUnit;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

public class SqlitePersistentImpressionsObserverCacheStorage implements PersistentImpressionsObserverCacheStorage {

    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final long mExpirationPeriod;

    public SqlitePersistentImpressionsObserverCacheStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao,
                                                           long expirationPeriod) {
        mImpressionsObserverCacheDao = checkNotNull(impressionsObserverCacheDao);
        mExpirationPeriod = expirationPeriod;
    }

    @Override
    @WorkerThread
    public void put(long hash, long time) {
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
        long oldestTimestamp = TimeUnit.SECONDS.toMillis(timestamp) - mExpirationPeriod;
        mImpressionsObserverCacheDao.deleteOldest(oldestTimestamp);
    }

    @Override
    public void onRemoval(Long key) {
        mImpressionsObserverCacheDao.delete(key);
    }
}
