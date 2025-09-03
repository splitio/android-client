package io.split.android.integration.storage;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.storage.cipher.NoOpCipher;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.integration.DatabaseHelper;

@RunWith(RobolectricTestRunner.class)
public class PersistentMyLargeSegmentStorageTest {
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private final String mUserKey = "userkey-1";

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();

        MyLargeSegmentEntity entity = new MyLargeSegmentEntity();
        entity.setUserKey(mUserKey);
        entity.setSegmentList("{\"k\":[{\"n\":\"s1\"},{\"n\":\"s2\"},{\"n\":\"s3\"}],\"cn\":null}");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(entity);

        entity = new MyLargeSegmentEntity();
        String mUserKey2 = "userkey-2";
        entity.setUserKey(mUserKey2);
        entity.setSegmentList("{\"k\":[{\"n\":\"s10\"},{\"n\":\"s20\"}],\"cn\":-1}");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(entity);

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false), mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());
    }

    @Test
    public void getMySegments() {
        SegmentsChange snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.getNames().size());
        Assert.assertTrue(snapshot.getNames().contains("s1"));
        Assert.assertTrue(snapshot.getNames().contains("s2"));
        Assert.assertTrue(snapshot.getNames().contains("s3"));
        Assert.assertNull(snapshot.getChangeNumber());
    }

    @Test
    public void updateSegments() {

        mPersistentMySegmentsStorage.set(mUserKey, SegmentsChange.create(Helper.asSet("a1", "a2", "a3", "a4"), 2002012));

        SegmentsChange snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(4, snapshot.getNames().size());
        Assert.assertTrue(snapshot.getNames().contains("a1"));
        Assert.assertTrue(snapshot.getNames().contains("a2"));
        Assert.assertTrue(snapshot.getNames().contains("a3"));
        Assert.assertTrue(snapshot.getNames().contains("a4"));
        Assert.assertEquals(2002012, snapshot.getChangeNumber().longValue());
    }

    @Test
    public void updateSegmentsEncrypted() {
        try (MockedStatic<SplitCipherFactory> mocked = mockStatic(SplitCipherFactory.class)) {
            mocked.when(() -> SplitCipherFactory.create(anyString(), eq(true)))
                    .thenReturn(new NoOpCipher());

            SplitCipher splitCipher = SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", true);
            mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(
                    splitCipher, mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());

            mPersistentMySegmentsStorage.set(mUserKey, SegmentsChange.create(Helper.asSet("a1", "a2", "a3", "a4"), 2002012));

            SegmentsChange snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

            List<String> mySegments = snapshot.getNames();
            Assert.assertEquals(4, mySegments.size());
            Assert.assertTrue(mySegments.contains("a1"));
            Assert.assertTrue(mySegments.contains("a2"));
            Assert.assertTrue(mySegments.contains("a3"));
            Assert.assertTrue(mySegments.contains("a4"));
            Assert.assertEquals(2002012, snapshot.getChangeNumber().longValue());
        }
    }

    @Test
    public void updateEmptyMySegment() {

        mPersistentMySegmentsStorage.set(mUserKey, SegmentsChange.create(Collections.emptySet(), 2002012));

        SegmentsChange snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(0, snapshot.getNames().size());
        Assert.assertEquals(2002012, snapshot.getChangeNumber().longValue());
    }

    @Test
    public void addNullMySegmentsList() {
        mPersistentMySegmentsStorage.set(mUserKey, SegmentsChange.create(null, 2002012));

        SegmentsChange snapshot = mPersistentMySegmentsStorage.getSnapshot(mUserKey);

        Assert.assertEquals(3, snapshot.getNames().size());
        Assert.assertNull(snapshot.getChangeNumber());
    }
}
