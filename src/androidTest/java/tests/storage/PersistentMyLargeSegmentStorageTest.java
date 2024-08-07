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
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SegmentChangeDTO;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;

public class PersistentMyLargeSegmentStorageTest {
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private final String mUserKey = "userkey-1";

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();

        MyLargeSegmentEntity entity = new MyLargeSegmentEntity();
        entity.setUserKey(mUserKey);
        entity.setSegmentList("s1,s2,s3");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(entity);

        entity = new MyLargeSegmentEntity();
        String mUserKey2 = "userkey-2";
        entity.setUserKey(mUserKey2);
        entity.setSegmentList("s10,s20");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(entity);

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false), mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());
    }

    @Test
    public void getMySegments() {
        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.getMySegments().size());
        Assert.assertTrue(snapshot.getMySegments().contains("s1"));
        Assert.assertTrue(snapshot.getMySegments().contains("s2"));
        Assert.assertTrue(snapshot.getMySegments().contains("s3"));
        Assert.assertEquals(-1, snapshot.getTill().longValue());
    }

    @Test
    public void updateSegments() {

        mPersistentMySegmentsStorage.set(mUserKey, Arrays.asList("a1","a2","a3","a4"), 2002012);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(4, snapshot.getMySegments().size());
        Assert.assertTrue(snapshot.getMySegments().contains("a1"));
        Assert.assertTrue(snapshot.getMySegments().contains("a2"));
        Assert.assertTrue(snapshot.getMySegments().contains("a3"));
        Assert.assertTrue(snapshot.getMySegments().contains("a4"));
        Assert.assertEquals(2002012, snapshot.getTill().longValue());
    }

    @Test
    public void updateSegmentsEncrypted() {
        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", true), mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());

        mPersistentMySegmentsStorage.set(mUserKey, Arrays.asList("a1","a2","a3","a4"), 2002012);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        List<String> mySegments = snapshot.getMySegments();
        Assert.assertEquals(4, mySegments.size());
        Assert.assertTrue(mySegments.contains("a1"));
        Assert.assertTrue(mySegments.contains("a2"));
        Assert.assertTrue(mySegments.contains("a3"));
        Assert.assertTrue(mySegments.contains("a4"));
        Assert.assertEquals(2002012, snapshot.getTill().longValue());
    }

    @Test
    public void updateEmptyMySegment() {

        mPersistentMySegmentsStorage.set(mUserKey, new ArrayList<>(), 2002012);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(0, snapshot.getMySegments().size());
        Assert.assertEquals(2002012, snapshot.getTill().longValue());
    }

    @Test
    public void addNullMySegmentsList() {
        mPersistentMySegmentsStorage.set(mUserKey, null, 2002012);

        SegmentChangeDTO snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.getMySegments().size());
        Assert.assertEquals(-1, snapshot.getTill().longValue());
    }
}
