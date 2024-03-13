package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class ImpressionsObserverCacheImplTest {

    private ImpressionsObserverCachePersistentStorage mPersistentStorage;
    private ListenableLruCache<Long, Long> mCache;
    private ImpressionsObserverCacheImpl mImpressionsObserverCacheImpl;

    @Before
    public void setUp() {
        mPersistentStorage = mock(ImpressionsObserverCachePersistentStorage.class);
        mCache = mock(ListenableLruCache.class);
        mImpressionsObserverCacheImpl = new ImpressionsObserverCacheImpl(mPersistentStorage, mCache);
    }

    @Test
    public void getReturnsValueFromCacheIfPresent() {
        when(mCache.get(1L)).thenReturn(2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
    }

    @Test
    public void getDoesNotCheckPersistentStorageIfValueIsPresentInCache() {
        when(mCache.get(1L)).thenReturn(2L);

        mImpressionsObserverCacheImpl.get(1L);

        verifyNoInteractions(mPersistentStorage);
    }

    @Test
    public void getReturnsValueFromPersistentStorageIfNotPresentInCache() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
    }

    @Test
    public void getChecksPersistentStorageIfValueNotPresentInCache() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(2L);

        mImpressionsObserverCacheImpl.get(1L);

        verify(mPersistentStorage).get(1L);
    }

    @Test
    public void getPutsValueInCacheIfValueExistsInPersistentStorage() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(2L);

        mImpressionsObserverCacheImpl.get(1L);

        verify(mCache).put(1L, 2L);
    }

    @Test
    public void getDoesNotPutValueInCacheIfNotPresentInPersistentStorage() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(null);

        mImpressionsObserverCacheImpl.get(1L);

        verify(mCache, times(0)).put(any(), any());
    }

    @Test
    public void putPutsValueInCacheAndPersistentStorage() {
        mImpressionsObserverCacheImpl.put(1L, 2L);

        verify(mCache).put(1L, 2L);
        verify(mPersistentStorage).insert(1L, 2L);
    }
}
