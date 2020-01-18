package storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsSnapshot;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.utils.StringHelper;

public class PersistentMySegmentStorageTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mContext.deleteDatabase("encripted_api_key");
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();


        String userKey = "userkey-1";
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList("s1,s2,s3");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.mySegmentDao().update(entity);

        entity = new MySegmentEntity();
        entity.setUserKey("userkey-2");
        entity.setSegmentList("s10,s20");
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mRoomDb.mySegmentDao().update(entity);

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(mRoomDb, userKey);
    }

    @Test
    public void getMySegments() {
        Set<String> snapshot = new HashSet(mPersistentMySegmentsStorage.getSnapshot());

        Assert.assertEquals(3, snapshot.size());
        Assert.assertTrue(snapshot.contains("s1"));
        Assert.assertTrue(snapshot.contains("s2"));
        Assert.assertTrue(snapshot.contains("s3"));
    }

    @Test
    public void updateSegments() {

        mPersistentMySegmentsStorage.set(Arrays.asList("a1,a2,a3,a4"));

        Set<String> snapshot = new HashSet<>(mPersistentMySegmentsStorage.getSnapshot());

        Assert.assertEquals(4, snapshot.size());
        Assert.assertTrue(snapshot.contains("a1"));
        Assert.assertTrue(snapshot.contains("a2"));
        Assert.assertTrue(snapshot.contains("a3"));
        Assert.assertTrue(snapshot.contains("a4"));
    }

    @Test
    public void updateEmptyMySegment() {
        List<String> splits = new ArrayList<>();

        mPersistentMySegmentsStorage.set(new ArrayList<>());

        List<String> snapshot = mPersistentMySegmentsStorage.getSnapshot();

        Assert.assertEquals(0, snapshot.size());
    }

    @Test
    public void addNullMySegmentsList() {
        mPersistentMySegmentsStorage.set(null);

        List<String> snapshot = mPersistentMySegmentsStorage.getSnapshot();

        Assert.assertEquals(3, snapshot.size());
    }

}