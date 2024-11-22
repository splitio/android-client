package io.split.android.client.service.impressions.observer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.utils.logger.Logger;

public class SqlitePersistentImpressionsObserverCacheStorage implements PersistentImpressionsObserverCacheStorage {

    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final long mExpirationPeriod;
    private final ScheduledExecutorService mExecutorsService;
    private final Map<Long, Long> mCache = new ConcurrentHashMap<>();
    private final AtomicBoolean mDelayedSyncRunning;
    private final PeriodicPersistenceTask.OnExecutedListener mCallback;

    public SqlitePersistentImpressionsObserverCacheStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao,
                                                           long expirationPeriod, ScheduledThreadPoolExecutor executorService) {
        this(impressionsObserverCacheDao,
                expirationPeriod,
                executorService,
                new AtomicBoolean(false));
    }

    @VisibleForTesting
    SqlitePersistentImpressionsObserverCacheStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao,
                                                    long expirationPeriod,
                                                    ScheduledExecutorService executorService,
                                                    AtomicBoolean delayedSyncRunning) {
        mImpressionsObserverCacheDao = checkNotNull(impressionsObserverCacheDao);
        mExpirationPeriod = expirationPeriod;
        mExecutorsService = executorService;
        mDelayedSyncRunning = delayedSyncRunning;
        mCallback = new PeriodicPersistenceTask.OnExecutedListener() {
            @Override
            public void onExecuted() {
                Logger.v("Impressions observer cache persisted");
                mDelayedSyncRunning.compareAndSet(true, false);
            }
        };
    }

    @Override
    @WorkerThread
    public void put(long hash, long time) {
        mCache.put(hash, time);
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
        long oldestTimestamp = timestamp - mExpirationPeriod;
        mImpressionsObserverCacheDao.deleteOldest(oldestTimestamp);
    }

    @Override
    public void onRemoval(Long key) {
        mCache.remove(key);
        mImpressionsObserverCacheDao.delete(key);
    }

    @Override
    public void persist() {
        if (mDelayedSyncRunning.compareAndSet(false, true)) {
            mExecutorsService.submit(new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mCallback));
        }
    }
}
