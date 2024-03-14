package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void getChecksPersistentStorageWhenCacheGetThrowsException() {
        when(mCache.get(1L)).thenThrow(new RuntimeException());
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
    public void getReturnsValueFromPersistedStorageWhenPutInCacheFails() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(2L);
        Mockito.doThrow(new RuntimeException()).when(mCache).put(1L, 2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
    }

    @Test
    public void getDoesNotPutValueInCacheIfNotPresentInPersistentStorage() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenReturn(null);

        mImpressionsObserverCacheImpl.get(1L);

        verify(mCache, times(0)).put(any(), any());
    }

    @Test
    public void getReturnsNullWhenValueNotPresentInCacheAndPersistentStorageGetFails() {
        when(mCache.get(1L)).thenReturn(null);
        when(mPersistentStorage.get(1L)).thenThrow(new RuntimeException());

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertNull(result);
    }

    @Test
    public void getReturnsValueFromCacheIfSecondHitSucceeds() {
        when(mCache.get(1L))
                .thenReturn(null)
                .thenReturn(2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        verifyNoInteractions(mPersistentStorage);
        assertEquals(2L, result.longValue());
    }

    @Test
    public void putPutsValueInCacheAndPersistentStorage() {
        mImpressionsObserverCacheImpl.put(1L, 2L);

        verify(mCache).put(1L, 2L);
        verify(mPersistentStorage).insert(1L, 2L);
    }

    @Test
    public void putStillPutsValueInPersistentStorageIfPutInCacheFails() {
        when(mCache.put(1L, 2L)).thenThrow(new RuntimeException());

        mImpressionsObserverCacheImpl.put(1L, 2L);

        verify(mPersistentStorage).insert(1L, 2L);
    }

    @Test
    public void putStillPutsValueInCacheIfPutInPersistentStorageFails() {
        doThrow(new RuntimeException()).when(mPersistentStorage).insert(1L, 2L);

        mImpressionsObserverCacheImpl.put(1L, 2L);

        verify(mCache).put(1L, 2L);
    }
}
