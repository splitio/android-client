package tests.database;

import static org.junit.Assert.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverEntity;

public class ImpressionsObserverDaoTest {

    private SplitRoomDatabase mTestDatabase;
    private ImpressionsObserverDao mImpressionsObserverDao;

    @Before
    public void setUp() {
        mTestDatabase = DatabaseHelper.getTestDatabase(InstrumentationRegistry.getInstrumentation().getContext());
        mImpressionsObserverDao = ImpressionsObserverDaoTest.this.mTestDatabase.impressionsObserverDao();
        mTestDatabase.clearAllTables();
    }

    @After
    public void tearDown() {
        mTestDatabase.clearAllTables();
    }

    @Test
    public void valuesAreInsertedCorrectly() {
        mImpressionsObserverDao.insert(1L, 2L, 3L);
        mImpressionsObserverDao.insert(3L, 2L, 5L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(3);

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
        mImpressionsObserverDao.insert(1L, 2L, 3L);
        mImpressionsObserverDao.insert(1L, 4L, 5L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 1L);
        assertEquals(firstEntity.getTime(), 4L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    @Test
    public void deleteRemovesCorrectHash() {
        mImpressionsObserverDao.insert(1L, 2L, 3L);
        mImpressionsObserverDao.insert(3L, 2L, 5L);
        mImpressionsObserverDao.delete(1L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(3);

        assertEquals(1, all.size());
        ImpressionsObserverEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getHash(), 3L);
        assertEquals(firstEntity.getTime(), 2L);
        assertEquals(firstEntity.getCreatedAt(), 5L);
    }

    @Test
    public void getAllWithLimitReturnsTheCorrectAmount() {
        mImpressionsObserverDao.insert(1L, 2L, 3L);
        mImpressionsObserverDao.insert(3L, 2L, 5L);
        mImpressionsObserverDao.insert(4L, 2L, 6L);
        mImpressionsObserverDao.insert(5L, 2L, 7L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(2);

        assertEquals(2, all.size());
    }

    @Test
    public void getAllReturnsElementsOrderedByCreatedAtAsc() {
        mImpressionsObserverDao.insert(3L, 2L, 3L);
        mImpressionsObserverDao.insert(4L, 6L, 5L);
        mImpressionsObserverDao.insert(5L, 4L, 6L);
        mImpressionsObserverDao.insert(1L, 1L, 7L);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(4);

        assertEquals(4, all.size());
        assertEquals(3L, all.get(0).getHash());
        assertEquals(4L, all.get(1).getHash());
        assertEquals(5L, all.get(2).getHash());
        assertEquals(1L, all.get(3).getHash());
    }

    @Test
    public void deleteOldestRemovesCorrectValues() {
        mImpressionsObserverDao.insert(3L, 2L, 3L);
        mImpressionsObserverDao.insert(4L, 6L, 5L);

        // only these ones should remain
        mImpressionsObserverDao.insert(5L, 4L, 6L);
        mImpressionsObserverDao.insert(12L, 4L, 7L);
        mImpressionsObserverDao.insert(21L, 3L, 8L);

        mImpressionsObserverDao.deleteOldest(6);

        List<ImpressionsObserverEntity> all = mImpressionsObserverDao.getAll(5);

        assertEquals(3, all.size());
        assertEquals(5L, all.get(0).getHash());
        assertEquals(12L, all.get(1).getHash());
        assertEquals(21L, all.get(2).getHash());
    }
}
