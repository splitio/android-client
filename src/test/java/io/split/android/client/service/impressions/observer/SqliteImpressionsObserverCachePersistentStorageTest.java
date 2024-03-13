package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.impressions.observer.SqliteImpressionsObserverCachePersistentStorage;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

public class SqliteImpressionsObserverCachePersistentStorageTest {

    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private SqliteImpressionsObserverCachePersistentStorage mStorage;

    @Before
    public void setUp() {
        mImpressionsObserverCacheDao = mock(ImpressionsObserverCacheDao.class);
        mStorage = new SqliteImpressionsObserverCachePersistentStorage(mImpressionsObserverCacheDao);
    }

    @Test
    public void insertInsertsInDao() {
        mStorage.insert(1, 2);

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
        mStorage.deleteOutdated(1);

        verify(mImpressionsObserverCacheDao).deleteOldest(1);
    }

    @Test
    public void onRemovalCallsDeleteOnDao() {
        mStorage.onRemoval(1L);

        verify(mImpressionsObserverCacheDao).delete(1L);
    }
}
