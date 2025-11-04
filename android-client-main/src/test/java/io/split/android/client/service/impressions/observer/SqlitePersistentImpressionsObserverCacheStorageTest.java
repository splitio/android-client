package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

public class SqlitePersistentImpressionsObserverCacheStorageTest {

    private static final int EXPIRATION_PERIOD = 2000;
    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private SqlitePersistentImpressionsObserverCacheStorage mStorage;
    private ScheduledExecutorService mExecutorService;
    private AtomicBoolean mDelayedSyncRunning;

    @Before
    public void setUp() {
        mImpressionsObserverCacheDao = mock(ImpressionsObserverCacheDao.class);
        mExecutorService = mock(ScheduledExecutorService.class);
        mDelayedSyncRunning = new AtomicBoolean(false);
        when(mExecutorService.submit(
                (Runnable) argThat(argument -> argument instanceof PeriodicPersistenceTask))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        mStorage = new SqlitePersistentImpressionsObserverCacheStorage(mImpressionsObserverCacheDao,
                EXPIRATION_PERIOD,
                mExecutorService,
                mDelayedSyncRunning);
    }

    @Test
    public void getGetsFromDao() {
        mStorage.get(1);

        verify(mImpressionsObserverCacheDao).get(1L);
    }

    @Test
    public void getReturnsNullWhenEntityIsNull() {
        when(mImpressionsObserverCacheDao.get(1L)).thenReturn(null);

        mStorage.get(1);

        verify(mImpressionsObserverCacheDao).get(1L);
    }

    @Test
    public void getReturnsTimeFromEntity() {
        when(mImpressionsObserverCacheDao.get(1L)).thenReturn(new ImpressionsObserverCacheEntity(1, 2, 3));

        Long value = mStorage.get(1);

        verify(mImpressionsObserverCacheDao).get(1L);
        assertEquals(2L, value.longValue());
    }

    @Test
    public void deleteOutdatedDeletesFromDao() {
        mStorage.deleteOutdated(3000);

        verify(mImpressionsObserverCacheDao).deleteOldest(3000 - EXPIRATION_PERIOD);
    }

    @Test
    public void onRemovalCallsDeleteOnDao() {
        mStorage.onRemoval(1L);

        verify(mImpressionsObserverCacheDao).delete(1L);
    }

    @Test
    public void putDoesNotCallPeriodicSync() {
        mStorage.put(1, 2);

        verify(mExecutorService, times(0)).submit(
                (Runnable) argThat(argument -> argument instanceof PeriodicPersistenceTask));
    }

    @Test
    public void multiplePersistCallsSubmitsOneTaskWhenPeriodicSyncIsTrue() {
        when(mExecutorService.submit(
                (Runnable) argThat(argument -> argument instanceof PeriodicPersistenceTask))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            mDelayedSyncRunning.set(true); //simulate task still running
            return null;
        });

        mStorage.put(1, 2);
        mStorage.put(3, 4);
        mStorage.persist();
        mStorage.persist();

        verify(mExecutorService, times(1)).submit(
                (Runnable) argThat(argument -> argument instanceof PeriodicPersistenceTask));
    }
}
