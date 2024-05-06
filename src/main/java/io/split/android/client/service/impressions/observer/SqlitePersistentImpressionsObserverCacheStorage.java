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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.utils.logger.Logger;

public class SqlitePersistentImpressionsObserverCacheStorage implements PersistentImpressionsObserverCacheStorage {

    private static final long DEFAULT_PERSISTENCE_DELAY = 1000;

    private final ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private final long mExpirationPeriod;
    private final long mPersistenceDelay;
    private final ScheduledExecutorService mExecutorsService;
    private final Map<Long, Long> mCache = new ConcurrentHashMap<>();
    private final AtomicBoolean mDelayedSyncRunning;
    private final PeriodicPersistenceTask.OnExecutedListener mCallback;

    public SqlitePersistentImpressionsObserverCacheStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao,
                                                           long expirationPeriod) {
        this(impressionsObserverCacheDao,
                expirationPeriod,
                DEFAULT_PERSISTENCE_DELAY,
                new ScheduledThreadPoolExecutor(1,
                        new ThreadPoolExecutor.CallerRunsPolicy()),
                new AtomicBoolean(false));
    }

    @VisibleForTesting
    SqlitePersistentImpressionsObserverCacheStorage(@NonNull ImpressionsObserverCacheDao impressionsObserverCacheDao,
                                                    long expirationPeriod,
                                                    long persistenceDelay,
                                                    ScheduledExecutorService executorService,
                                                    AtomicBoolean delayedSyncRunning) {
        mImpressionsObserverCacheDao = checkNotNull(impressionsObserverCacheDao);
        mExpirationPeriod = expirationPeriod;
        mPersistenceDelay = persistenceDelay;
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

        // If the task is not running, schedule it
        if (mDelayedSyncRunning.compareAndSet(false, true)) {
            mExecutorsService.schedule(new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mCallback),
                    mPersistenceDelay,
                    TimeUnit.MILLISECONDS);
        }
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
}
