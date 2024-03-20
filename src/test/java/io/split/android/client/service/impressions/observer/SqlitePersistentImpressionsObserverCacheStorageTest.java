package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

public class SqlitePersistentImpressionsObserverCacheStorageTest {

    private static final int EXPIRATION_PERIOD = 2000;
    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private SqlitePersistentImpressionsObserverCacheStorage mStorage;

    @Before
    public void setUp() {
        mImpressionsObserverCacheDao = mock(ImpressionsObserverCacheDao.class);
        mStorage = new SqlitePersistentImpressionsObserverCacheStorage(mImpressionsObserverCacheDao, EXPIRATION_PERIOD);
    }

    @Test
    public void insertInsertsInDao() {
        mStorage.put(1, 2);

        verify(mImpressionsObserverCacheDao).insert(eq(1L), eq(2L), argThat(argument -> argument > 0));
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
        int timestampSeconds = 3;
        mStorage.deleteOutdated(timestampSeconds);

        verify(mImpressionsObserverCacheDao).deleteOldest(TimeUnit.SECONDS.toMillis(timestampSeconds) - EXPIRATION_PERIOD);
    }

    @Test
    public void onRemovalCallsDeleteOnDao() {
        mStorage.onRemoval(1L);

        verify(mImpressionsObserverCacheDao).delete(1L);
    }
}
