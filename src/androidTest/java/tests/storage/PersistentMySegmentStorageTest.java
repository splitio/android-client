package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;

public class PersistentMySegmentStorageTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private final String mUserKey = "userkey-1";

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();

        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(mUserKey);
        entity.setSegmentList("s1,s2,s3");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.mySegmentDao().update(entity);

        entity = new MySegmentEntity();
        String mUserKey2 = "userkey-2";
        entity.setUserKey(mUserKey2);
        entity.setSegmentList("s10,s20");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.mySegmentDao().update(entity);

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false), mRoomDb.mySegmentDao(), MySegmentEntity.creator());
    }

    @Test
    public void getMySegments() {
        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        List<String> mySegments = snapshot.getMySegments();
        Assert.assertEquals(3, mySegments.size());
        Assert.assertTrue(mySegments.contains("s1"));
        Assert.assertTrue(mySegments.contains("s2"));
        Assert.assertTrue(mySegments.contains("s3"));
    }

    @Test
    public void updateSegments() {

        mPersistentMySegmentsStorage.set(mUserKey, Arrays.asList("a1", "a2", "a3", "a4"), 2002012);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        List<String> mySegments = snapshot.getMySegments();
        Long till = snapshot.getTill();
        Assert.assertEquals(4, mySegments.size());
        Assert.assertTrue(mySegments.contains("a1"));
        Assert.assertTrue(mySegments.contains("a2"));
        Assert.assertTrue(mySegments.contains("a3"));
        Assert.assertTrue(mySegments.contains("a4"));
        Assert.assertEquals(2002012, till.longValue());
    }

    @Test
    public void updateSegmentsEncrypted() {
        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", true), mRoomDb.mySegmentDao(), MySegmentEntity.creator());

        mPersistentMySegmentsStorage.set(mUserKey, Arrays.asList("a1", "a2", "a3", "a4"), -1);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        List<String> mySegments = snapshot.getMySegments();
        Assert.assertEquals(4, mySegments.size());
        Assert.assertTrue(mySegments.contains("a1"));
        Assert.assertTrue(mySegments.contains("a2"));
        Assert.assertTrue(mySegments.contains("a3"));
        Assert.assertTrue(mySegments.contains("a4"));
        Assert.assertEquals(-1, snapshot.getTill().longValue());
    }

    @Test
    public void updateEmptyMySegment() {

        mPersistentMySegmentsStorage.set(mUserKey, new ArrayList<>(), 22121);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(0, snapshot.getMySegments().size());
        Assert.assertEquals(22121, snapshot.getTill().longValue());
    }

    @Test
    public void addNullMySegmentsList() {
        mPersistentMySegmentsStorage.set(mUserKey, null, -1);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.getMySegments().size());
        Assert.assertEquals(-1, snapshot.getTill().longValue());
    }
}
