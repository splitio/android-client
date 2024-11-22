package tests.database;

import static org.junit.Assert.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

public class ImpressionsObserverCacheDaoTest {

    private SplitRoomDatabase mTestDatabase;
    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;

    @Before
    public void setUp() {
        mTestDatabase = DatabaseHelper.getTestDatabase(InstrumentationRegistry.getInstrumentation().getContext());
        mImpressionsObserverCacheDao = mTestDatabase.impressionsObserverCacheDao();
        mTestDatabase.clearAllTables();
    }

    @After
    public void tearDown() {
        mTestDatabase.clearAllTables();
    }

    @Test
    public void valuesAreInsertedCorrectly() {
        insertIntoDao();

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(2, all.size());
        ImpressionsObserverCacheEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 1L);
        assertEquals(firstEntity.getTime(), 2L);
        assertEquals(firstEntity.getCreatedAt(), 3L);
        ImpressionsObserverCacheEntity secondEntity = all.get(1);
        assertEquals(secondEntity.getHash(), 3L);
        assertEquals(secondEntity.getTime(), 2L);
        assertEquals(secondEntity.getCreatedAt(), 5L);
    }

    private void insertIntoDao() {
        List<ImpressionsObserverCacheEntity> entities = Arrays.asList(
                new ImpressionsObserverCacheEntity(1L, 2L, 3L),
                new ImpressionsObserverCacheEntity(3L, 2L, 5L));
        mImpressionsObserverCacheDao.insert(entities);
    }

    @Test
    public void valueWithNewHashReplacesOldOne() {
        insertIntoDaoOnce(1L, 2L, 3L);
        insertIntoDaoOnce(1L, 4L, 5L);

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverCacheEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 1L);
        assertEquals(firstEntity.getTime(), 4L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    private void insertIntoDaoOnce(long hash, long time, long createdAt) {
        ImpressionsObserverCacheEntity entity = new ImpressionsObserverCacheEntity(hash, time, createdAt);
        mImpressionsObserverCacheDao.insert(Arrays.asList(entity));
    }

    @Test
    public void deleteRemovesCorrectHash() {
        insertIntoDao();
        mImpressionsObserverCacheDao.delete(1L);

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverCacheEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 3L);
        assertEquals(firstEntity.getTime(), 2L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    @Test
    public void getAllWithLimitReturnsTheCorrectAmount() {
        insertIntoDao();
        insertIntoDaoOnce(4L, 2L, 6L);
        insertIntoDaoOnce(5L, 2L, 7L);

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(2);

        assertEquals(2, all.size());
    }

    @Test
    public void getAllReturnsElementsOrderedByCreatedAtAsc() {
        insertIntoDaoOnce(3L, 2L, 3L);
        insertIntoDaoOnce(4L, 6L, 5L);
        insertIntoDaoOnce(5L, 4L, 6L);
        insertIntoDaoOnce(1L, 1L, 7L);

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(4);

        assertEquals(4, all.size());
        assertEquals(3L, all.get(0).getHash());
        assertEquals(4L, all.get(1).getHash());
        assertEquals(5L, all.get(2).getHash());
        assertEquals(1L, all.get(3).getHash());
    }

    @Test
    public void deleteOldestRemovesCorrectValues() {
        insertIntoDaoOnce(3L, 2L, 3L);
        insertIntoDaoOnce(4L, 6L, 5L);

        // only these ones should remain
        insertIntoDaoOnce(5L, 4L, 6L);
        insertIntoDaoOnce(12L, 4L, 7L);
        insertIntoDaoOnce(21L, 3L, 8L);

        mImpressionsObserverCacheDao.deleteOldest(6);

        List<ImpressionsObserverCacheEntity> all = mImpressionsObserverCacheDao.getAll(5);

        assertEquals(3, all.size());
        assertEquals(5L, all.get(0).getHash());
        assertEquals(12L, all.get(1).getHash());
        assertEquals(21L, all.get(2).getHash());
    }

    @Test
    public void getSingleValueReturnsCorrectValue() {
        insertIntoDaoOnce(3L, 2L, 3L);
        insertIntoDaoOnce(4L, 6L, 5L);

        ImpressionsObserverCacheEntity entity = mImpressionsObserverCacheDao.get(3L);

        assertEquals(3L, entity.getHash());
        assertEquals(2L, entity.getTime());
    }
}
