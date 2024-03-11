package tests.database;

import static org.junit.Assert.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverEntity;

public class ImpressionsObserverCacheDaoTest {

    private SplitRoomDatabase mTestDatabase;
    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;

    @Before
    public void setUp() {
        mTestDatabase = DatabaseHelper.getTestDatabase(InstrumentationRegistry.getInstrumentation().getContext());
        mImpressionsObserverCacheDao = ImpressionsObserverCacheDaoTest.this.mTestDatabase.impressionsObserverDao();
        mTestDatabase.clearAllTables();
    }

    @After
    public void tearDown() {
        mTestDatabase.clearAllTables();
    }

    @Test
    public void valuesAreInsertedCorrectly() {
        mImpressionsObserverCacheDao.insert(1L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(3L, 2L, 5L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(2, all.size());
        ImpressionsObserverEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 1L);
        assertEquals(firstEntity.getTime(), 2L);
        assertEquals(firstEntity.getCreatedAt(), 3L);
        ImpressionsObserverEntity secondEntity = all.get(1);
        assertEquals(secondEntity.getHash(), 3L);
        assertEquals(secondEntity.getTime(), 2L);
        assertEquals(secondEntity.getCreatedAt(), 5L);
    }

    @Test
    public void valueWithNewHashReplacesOldOne() {
        mImpressionsObserverCacheDao.insert(1L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(1L, 4L, 5L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 1L);
        assertEquals(firstEntity.getTime(), 4L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    @Test
    public void deleteRemovesCorrectHash() {
        mImpressionsObserverCacheDao.insert(1L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(3L, 2L, 5L);
        mImpressionsObserverCacheDao.delete(1L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 3L);
        assertEquals(firstEntity.getTime(), 2L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    @Test
    public void getAllWithLimitReturnsTheCorrectAmount() {
        mImpressionsObserverCacheDao.insert(1L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(3L, 2L, 5L);
        mImpressionsObserverCacheDao.insert(4L, 2L, 6L);
        mImpressionsObserverCacheDao.insert(5L, 2L, 7L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(2);

        assertEquals(2, all.size());
    }

    @Test
    public void getAllReturnsElementsOrderedByCreatedAtAsc() {
        mImpressionsObserverCacheDao.insert(3L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(4L, 6L, 5L);
        mImpressionsObserverCacheDao.insert(5L, 4L, 6L);
        mImpressionsObserverCacheDao.insert(1L, 1L, 7L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(4);

        assertEquals(4, all.size());
        assertEquals(3L, all.get(0).getHash());
        assertEquals(4L, all.get(1).getHash());
        assertEquals(5L, all.get(2).getHash());
        assertEquals(1L, all.get(3).getHash());
    }

    @Test
    public void deleteOldestRemovesCorrectValues() {
        mImpressionsObserverCacheDao.insert(3L, 2L, 3L);
        mImpressionsObserverCacheDao.insert(4L, 6L, 5L);

        // only these ones should remain
        mImpressionsObserverCacheDao.insert(5L, 4L, 6L);
        mImpressionsObserverCacheDao.insert(12L, 4L, 7L);
        mImpressionsObserverCacheDao.insert(21L, 3L, 8L);

        mImpressionsObserverCacheDao.deleteOldest(6);

        List<ImpressionsObserverEntity> all = mImpressionsObserverCacheDao.getAll(5);

        assertEquals(3, all.size());
        assertEquals(5L, all.get(0).getHash());
        assertEquals(12L, all.get(1).getHash());
        assertEquals(21L, all.get(2).getHash());
    }
}
