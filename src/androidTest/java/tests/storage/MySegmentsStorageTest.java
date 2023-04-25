package tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainerImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;

public class MySegmentsStorageTest {

    private SplitRoomDatabase mRoomDb;
    private Context mContext;
    private PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private MySegmentsStorage mMySegmentsStorage;
    private final String mUserKey = "userkey-1";
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

        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(mRoomDb,
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", false));
        mMySegmentsStorageContainer = new MySegmentsStorageContainerImpl(mPersistentMySegmentsStorage);
        mMySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
    }

    @Test
    public void noLocalLoaded() {
        Set<String> snapshot = new HashSet(mMySegmentsStorage.getAll());

        assertEquals(0, snapshot.size());
    }

    @Test
    public void getMySegments() {
        mMySegmentsStorage.loadLocal();
        Set<String> snapshot = new HashSet(mMySegmentsStorage.getAll());

        assertEquals(3, snapshot.size());
        assertTrue(snapshot.contains("s1"));
        assertTrue(snapshot.contains("s2"));
        assertTrue(snapshot.contains("s3"));
    }

    @Test
    public void updateSegments() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.set(Arrays.asList("a1", "a2", "a3", "a4"));
        MySegmentsStorage mySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        assertEquals(4, snapshot.size());
        assertTrue(snapshot.contains("a1"));
        assertTrue(snapshot.contains("a2"));
        assertTrue(snapshot.contains("a3"));
        assertTrue(snapshot.contains("a4"));

        assertEquals(4, newSnapshot.size());
        assertTrue(newSnapshot.contains("a1"));
        assertTrue(newSnapshot.contains("a2"));
        assertTrue(newSnapshot.contains("a3"));
        assertTrue(newSnapshot.contains("a4"));
    }

    @Test
    public void updateEmptyMySegment() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.set(new ArrayList<>());

        MySegmentsStorage mySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        assertEquals(0, snapshot.size());
        assertEquals(0, newSnapshot.size());
    }

    @Test
    public void addNullMySegmentsList() {

        mPersistentMySegmentsStorage.set(mUserKey, null);
        mMySegmentsStorage.loadLocal();
        MySegmentsStorage mySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());
        Set<String> newSnapshot = new HashSet<>(mySegmentsStorage.getAll());

        assertEquals(3, snapshot.size());
        assertEquals(3, newSnapshot.size());
    }

    @Test
    public void clear() {
        mMySegmentsStorage.loadLocal();
        mMySegmentsStorage.clear();
        mMySegmentsStorage.loadLocal();

        MySegmentsStorage mySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mySegmentsStorage.loadLocal();

        Set<String> snapshot = new HashSet<>(mMySegmentsStorage.getAll());

        assertEquals(0, snapshot.size());
    }

    @Test
    public void originalValuesCanBeRetrievedWhenStorageIsEncrypted() {
        mPersistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(mRoomDb,
                SplitCipherFactory.create("abcdefghijlkmnopqrstuvxyz", true));
        mMySegmentsStorageContainer = new MySegmentsStorageContainerImpl(mPersistentMySegmentsStorage);
        mMySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);

        mMySegmentsStorage.set(Arrays.asList("a1", "a2", "a3", "a4"));
        MySegmentsStorage mySegmentsStorage = mMySegmentsStorageContainer.getStorageForKey(mUserKey);
        mySegmentsStorage.loadLocal();

        Set<String> all = mySegmentsStorage.getAll();
        assertTrue(all.contains("a1"));
        assertTrue(all.contains("a2"));
        assertTrue(all.contains("a3"));
        assertTrue(all.contains("a4"));
        assertEquals(4, all.size());
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
                        mMySegmentsStorage.set(segments);
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                    }
                }
                latch.countDown();
            }
        }).start();
        latch.await(40, TimeUnit.SECONDS);
        Set<String> l = mMySegmentsStorage.getAll();
        assertEquals(10, mMySegmentsStorage.getAll().size());
    }
}
