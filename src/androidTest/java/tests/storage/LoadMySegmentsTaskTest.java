package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import helper.DatabaseHelper;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTaskConfig;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainerImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;

public class LoadMySegmentsTaskTest {
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    PersistentMySegmentsStorage mPersistentMyLargeSegmentsStorage;
    MySegmentsStorage mMySegmentsStorage;
    MySegmentsStorage mMyLargeSegmentsStorage;
    final String mUserKey = "userkey-1";
    private MySegmentsStorageContainer mMySegmentsStorageContainer;
    private MySegmentsStorageContainer mMyLargeSegmentsStorageContainer;

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
        entity.setUserKey("userkey-2");
        entity.setSegmentList("s10,s20");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.mySegmentDao().update(entity);

        MyLargeSegmentEntity largeEntity = new MyLargeSegmentEntity();
        largeEntity.setUserKey(mUserKey);
        largeEntity.setSegmentList("ls1,ls2,ls3");
        largeEntity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(largeEntity);

        largeEntity = new MyLargeSegmentEntity();
        largeEntity.setUserKey("userkey-2");
        largeEntity.setSegmentList("ls10,ls20");
        largeEntity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.myLargeSegmentDao().update(largeEntity);

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false), mRoomDb.mySegmentDao(), MySegmentEntity.creator());
        mPersistentMyLargeSegmentsStorage = new SqLitePersistentMySegmentsStorage(SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false), mRoomDb.myLargeSegmentDao(), MyLargeSegmentEntity.creator());
        mMySegmentsStorageContainer = new MySegmentsStorageContainerImpl(mPersistentMySegmentsStorage);
        mMyLargeSegmentsStorageContainer = new MySegmentsStorageContainerImpl(mPersistentMyLargeSegmentsStorage);
        mMySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mMyLargeSegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
    }

    @Test
    public void execute() {

        SplitTask task = new LoadMySegmentsTask(mMySegmentsStorage, mMyLargeSegmentsStorage, LoadMySegmentsTaskConfig.get());
        SplitTaskExecutionInfo result = task.execute();
        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> largeSnapshot = new HashSet<>(mMyLargeSegmentsStorage.getAll());

        Assert.assertEquals(3, snapshot.size());
        Assert.assertTrue(snapshot.contains("s1"));
        Assert.assertTrue(snapshot.contains("s2"));
        Assert.assertTrue(snapshot.contains("s3"));

        Assert.assertEquals(3, largeSnapshot.size());
        Assert.assertTrue(largeSnapshot.contains("ls1"));
        Assert.assertTrue(largeSnapshot.contains("ls2"));
        Assert.assertTrue(largeSnapshot.contains("ls3"));
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }
}
