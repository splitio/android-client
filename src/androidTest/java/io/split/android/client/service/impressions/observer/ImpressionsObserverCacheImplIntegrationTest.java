package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;

public class ImpressionsObserverCacheImplIntegrationTest {

    private ImpressionsObserverCacheImpl mImpressionsObserverCacheImpl;
    private ListenableLruCache<Long, Long> mCache;
    private PersistentImpressionsObserverCacheStorage mPersistentStorage;

    @Before
    public void setUp() {
        ImpressionsObserverCacheDao impressionsObserverCacheDao = DatabaseHelper.getTestDatabase(InstrumentationRegistry.getInstrumentation().getContext()).impressionsObserverCacheDao();
        mPersistentStorage = new SqlitePersistentImpressionsObserverCacheStorage(impressionsObserverCacheDao);
        mCache = new ListenableLruCache<>(5, mPersistentStorage);
        mImpressionsObserverCacheImpl = new ImpressionsObserverCacheImpl(mPersistentStorage, mCache);
    }

    @Test
    public void getReturnsValueFromCacheIfPresent() {
        mCache.put(1L, 2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
    }

    @Test
    public void getReturnsValueFromPersistentStorageIfNotPresentInCache() {
        mPersistentStorage.put(1L, 2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
    }

    @Test
    public void leastRecentlyUsedValueIsRemovedFromDatabaseWhenLimitIsReached() {
        for (int i = 0; i < 6; i++) {
            mImpressionsObserverCacheImpl.put(i, i);
        }

        Map<Long, Long> values = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            Long value = mImpressionsObserverCacheImpl.get(i);
            if (value != null) {
                values.put((long) i, value);
            }
        }

        assertEquals(5, values.size());
        assertFalse(values.containsKey(0L));
        assertTrue(values.containsKey(1L));
        assertTrue(values.containsKey(2L));
        assertTrue(values.containsKey(3L));
        assertTrue(values.containsKey(4L));
        assertTrue(values.containsKey(5L));
    }

    @Test
    public void getPutsValueInCacheWhenRetrievedFromPersistentStorage() {
        mPersistentStorage.put(1L, 2L);

        Long result = mImpressionsObserverCacheImpl.get(1L);

        assertEquals(2L, result.longValue());
        assertEquals(2L, mCache.get(1L).longValue());
    }

    @Test
    public void putPutsValueInCacheAndPersistentStorage() {
        mImpressionsObserverCacheImpl.put(1L, 2L);

        assertEquals(2L, mPersistentStorage.get(1L).longValue());
        assertEquals(2L, mCache.get(1L).longValue());
    }

    @Test
    public void putUpdatesValueInCacheAndPersistentStorage() {
        mImpressionsObserverCacheImpl.put(1L, 2L);
        mImpressionsObserverCacheImpl.put(1L, 3L);

        assertEquals(3L, mPersistentStorage.get(1L).longValue());
        assertEquals(3L, mCache.get(1L).longValue());
    }
}
