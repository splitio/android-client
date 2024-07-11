package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import helper.DatabaseHelper;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
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
        Set<String> snapshot = new HashSet<>(mPersistentMySegmentsStorage.getSnapshot(mUserKey));

        Assert.assertEquals(3, snapshot.size());
        Assert.assertTrue(snapshot.contains("s1"));
        Assert.assertTrue(snapshot.contains("s2"));
        Assert.assertTrue(snapshot.contains("s3"));
    }

    @Test
    public void updateSegments() {

        mPersistentMySegmentsStorage.set(mUserKey, Collections.singletonList("a1,a2,a3,a4"));

        Set<String> snapshot = new HashSet<>(mPersistentMySegmentsStorage.getSnapshot(mUserKey));

        Assert.assertEquals(4, snapshot.size());
        Assert.assertTrue(snapshot.contains("a1"));
        Assert.assertTrue(snapshot.contains("a2"));
        Assert.assertTrue(snapshot.contains("a3"));
        Assert.assertTrue(snapshot.contains("a4"));
    }

    @Test
    public void updateSegmentsEncrypted() {
        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", true), mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());

        mPersistentMySegmentsStorage.set(mUserKey, Collections.singletonList("a1,a2,a3,a4"));

        Set<String> snapshot = new HashSet<>(mPersistentMySegmentsStorage.getSnapshot(mUserKey));

        Assert.assertEquals(4, snapshot.size());
        Assert.assertTrue(snapshot.contains("a1"));
        Assert.assertTrue(snapshot.contains("a2"));
        Assert.assertTrue(snapshot.contains("a3"));
        Assert.assertTrue(snapshot.contains("a4"));
    }

    @Test
    public void updateEmptyMySegment() {

        mPersistentMySegmentsStorage.set(mUserKey, new ArrayList<>());

        List<String> snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(0, snapshot.size());
    }

    @Test
    public void addNullMySegmentsList() {
        mPersistentMySegmentsStorage.set(mUserKey, null);

        List<String> snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.size());
    }
}
