package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;

public class MySegmentsStorageTest {
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    MySegmentsStorage mMySegmentsStorage;
    final String mUserKey = "userkey-1";

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mContext.deleteDatabase("encripted_api_key");
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
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

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(mRoomDb, mUserKey);
        mMySegmentsStorage = new MySegmentsStorageImpl(mPersistentMySegmentsStorage);
    }

    @Test
    public void noLocalLoaded() {
        Set<String> snapshot = new HashSet(mMySegmentsStorage.getAll());

        Assert.assertEquals(0, snapshot.size());
    }

    @Test
    public void getMySegments() {
        mMySegmentsStorage.loadLocal();
        Set<String> snapshot = new HashSet(mMySegmentsStorage.getAll());

        Assert.assertEquals(3, snapshot.size());
        Assert.assertTrue(snapshot.contains("s1"));
        Assert.assertTrue(snapshot.contains("s2"));
        Assert.assertTrue(snapshot.contains("s3"));
    }

    @Test
    public void updateSegments() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.set(Arrays.asList("a1", "a2", "a3", "a4"));
        MySegmentsStorageImpl mySegmentsStorage = new MySegmentsStorageImpl(mPersistentMySegmentsStorage);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        Assert.assertEquals(4, snapshot.size());
        Assert.assertTrue(snapshot.contains("a1"));
        Assert.assertTrue(snapshot.contains("a2"));
        Assert.assertTrue(snapshot.contains("a3"));
        Assert.assertTrue(snapshot.contains("a4"));

        Assert.assertEquals(4, newSnapshot.size());
        Assert.assertTrue(newSnapshot.contains("a1"));
        Assert.assertTrue(newSnapshot.contains("a2"));
        Assert.assertTrue(newSnapshot.contains("a3"));
        Assert.assertTrue(newSnapshot.contains("a4"));
    }

    @Test
    public void updateEmptyMySegment() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.set(new ArrayList<>());

        MySegmentsStorageImpl mySegmentsStorage = new MySegmentsStorageImpl(mPersistentMySegmentsStorage);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        Assert.assertEquals(0, snapshot.size());
        Assert.assertEquals(0, newSnapshot.size());
    }

    @Test
    public void addNullMySegmentsList() {

        mPersistentMySegmentsStorage.set(null);
        mMySegmentsStorage.loadLocal();
        MySegmentsStorageImpl mySegmentsStorage = new MySegmentsStorageImpl(mPersistentMySegmentsStorage);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        Assert.assertEquals(3, snapshot.size());
        Assert.assertEquals(3, newSnapshot.size());
    }

    @Test
    public void clear() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.clear();
        mMySegmentsStorage.loadLocal();

        MySegmentsStorageImpl mySegmentsStorage = new MySegmentsStorageImpl(mPersistentMySegmentsStorage);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        Assert.assertEquals(0, snapshot.size());
    }

    @Test
    public void updateToStorageConcurrency() throws InterruptedException {
        mMySegmentsStorage.loadLocal();
        CountDownLatch latch = new CountDownLatch(2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int j = 1000; j < 1200; j += 10) {
                    List<String> segments = new ArrayList<>();

                    for (int i = 0; i < 10; i++) {
                        segments.add("segment_" + j + "_" + i);
                    }
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                    }
                    mMySegmentsStorage.set(segments);
                }
                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int j = 0; j < 200; j += 10) {
                    List<String> segments = new ArrayList<>();

                    for (int i = 0; i < 10; i++) {
                        segments.add("segment_" + j + "_" + i);
                    }
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                    }
                    mMySegmentsStorage.set(segments);
                }
                latch.countDown();
            }
        }).start();
        latch.await(40, TimeUnit.SECONDS);
        Set<String> l = mMySegmentsStorage.getAll();
        Assert.assertEquals(10, mMySegmentsStorage.getAll().size());
    }

}
