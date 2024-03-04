package tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.storage.db.ImpressionsObserverEntity;
import io.split.android.client.storage.db.ImpressionsObserverDao;

public class ImpressionsObserverDaoTest extends GenericDaoTest {

    private ImpressionsObserverDao mImpressionsDedupeDao;

    @Before
    public void setup() {
        super.setup();
        mImpressionsDedupeDao = mRoomDb.impressionsDedupeDao();
    }

    @Test
    public void testInsert() {

//        List<ImpressionsObserverEntity> initialElements = mImpressionsDedupeDao.getAll();
//
//        mImpressionsDedupeDao.insert(1L, 2L);
//
//        List<ImpressionsObserverEntity> finalElements = mImpressionsDedupeDao.getAll();
//
//        assertEquals(0, initialElements.size());
//        assertEquals(1, finalElements.size());
    }

    @Test
    public void testGet() {

//        mImpressionsDedupeDao.insert(1L, 2L);
//
//        Long time = mImpressionsDedupeDao.get(1L);
//
//        assertEquals(2L, time.longValue());
    }

    @Test
    public void testDelete() {

//        mImpressionsDedupeDao.insert(1L, 2L);
//        Long firstTime = mImpressionsDedupeDao.get(1L);
//
//        mImpressionsDedupeDao.delete(1L);
//
//        Long time = mImpressionsDedupeDao.get(1L);
//
//        assertEquals(2L, firstTime.longValue());
//        assertNull(time);
    }
}
