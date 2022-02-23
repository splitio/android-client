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
    MySegmentsStorage mMySegmentsStorage;
    final String mUserKey = "userkey-1";
    private MySegmentsStorageContainer mMySegmentsStorageContainer;

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

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(mRoomDb);
        mMySegmentsStorageContainer = new MySegmentsStorageContainerImpl(mPersistentMySegmentsStorage);
        mMySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
    }

    @Test
    public void execute() {

        SplitTask task = new LoadMySegmentsTask(mMySegmentsStorage);
        SplitTaskExecutionInfo result = task.execute();
        Set<String> snapshot = new HashSet(mMySegmentsStorage.getAll());

        Assert.assertEquals(3, snapshot.size());
        Assert.assertTrue(snapshot.contains("s1"));
        Assert.assertTrue(snapshot.contains("s2"));
        Assert.assertTrue(snapshot.contains("s3"));
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }
}
