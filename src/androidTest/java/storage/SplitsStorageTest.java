package storage;

import android.content.Context;

import androidx.room.RoomDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonSyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.RoomSqLitePersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SplitsStorageTest {

    final static Long INITIAL_CHANGE_NUMBER = 9999L;
    final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d}";
    SplitRoomDatabase mRoomDb;
    Context mContext;
    SplitsStorage mSplitsStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
        List<SplitEntity> entities = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(JSON_SPLIT_TEMPLATE, splitName, INITIAL_CHANGE_NUMBER - i));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, INITIAL_CHANGE_NUMBER));
        mSplitsStorage = new SplitsStorageImpl(new RoomSqLitePersistentSplitsStorage(mRoomDb));
    }

    @Test
    public void getSplits() {

        Split split0 = mSplitsStorage.get("split-0");
        Split split1 = mSplitsStorage.get("split-1");
        Split split2 = mSplitsStorage.get("split-2");
        Split split3 = mSplitsStorage.get("split-3");

        Assert.assertNotNull(split0);
        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(split3);
    }

    @Test
    public void addSplits() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String splitName = "split-test-" + i;
            Split split = new Split();
            split.name = splitName;
            split.status = Status.ACTIVE;
            splits.add(split);
        }
        mSplitsStorage.update(splits, 1L);

        for (int i = 0; i < 4; i++) {
            String splitName = "split-test-" + i;
            Split splitTest = mSplitsStorage.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
    }

    @Test
    public void initialChangeNumber() {
        Assert.assertEquals((long) INITIAL_CHANGE_NUMBER, mSplitsStorage.getTill());
    }

    @Test
    public void updateChangeNumber() {
        Long newChangeNumber = INITIAL_CHANGE_NUMBER + 100;
        Long initialChangeNumber = mSplitsStorage.getTill();
        mSplitsStorage.update(new ArrayList<>(), newChangeNumber);
        Long updatedChangeNumber = mSplitsStorage.getTill();
        Assert.assertEquals(INITIAL_CHANGE_NUMBER, initialChangeNumber);
        Assert.assertEquals(newChangeNumber, updatedChangeNumber);
    }

    @Test
    public void updateToStorageConcurrency() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int j = 1000; j < 1200; j += 10) {
                    List<Split> splits = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        int p = j + i;
                        String splitName = "split-test-" + p;
                        Split split = new Split();
                        split.name = splitName;
                        split.status = (p % 2 == 0 ? Status.ACTIVE : Status.ARCHIVED);
                        splits.add(split);
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                        }
                    }
                    mSplitsStorage.update(splits, 1L);
                }
                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int j = 0; j < 200; j += 10) {
                    List<Split> splits = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        int p = j + i;
                        String splitName = "split-test-" + p;
                        Split split = new Split();
                        split.name = splitName;
                        split.status = (p % 2 != 0 ? Status.ACTIVE : Status.ARCHIVED);
                        splits.add(split);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    mSplitsStorage.update(splits, 1L);
                }
                latch.countDown();
            }
        }).start();
        latch.await(40, TimeUnit.SECONDS);

        Assert.assertNotNull(mSplitsStorage.get("split-test-1000"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-1100"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-1198"));
        Assert.assertNull(mSplitsStorage.get("split-test-1001"));
        Assert.assertNull(mSplitsStorage.get("split-test-1101"));
        Assert.assertNull(mSplitsStorage.get("split-test-1199"));

        Assert.assertNotNull(mSplitsStorage.get("split-test-1"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-101"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-199"));
        Assert.assertNull(mSplitsStorage.get("split-test-0"));
        Assert.assertNull(mSplitsStorage.get("split-test-100"));
        Assert.assertNull(mSplitsStorage.get("split-test-198"));

    }

    @Test
    public void updatedSplitTrafficType() {
        Split s1 = newSplit("s1", Status.ACTIVE, "tt");

        Split s2 = newSplit("s2", Status.ACTIVE, "mytt");
        Split s2ar = newSplit("s2", Status.ARCHIVED, "mytt");

        mSplitsStorage.update(Arrays.asList(s1), 1L);
        mSplitsStorage.update(Arrays.asList(s2), 1L);
        mSplitsStorage.update(Arrays.asList(s2), 1L);
        mSplitsStorage.update(Arrays.asList(s2), 1L);
        mSplitsStorage.update(Arrays.asList(s2ar), 1L);

        Assert.assertTrue(mSplitsStorage.isValidTrafficType("tt"));
        Assert.assertFalse(mSplitsStorage.isValidTrafficType("mytt"));
    }

    @Test
    public void changedTrafficTypeForSplit() {
        String splitName = "n_s1";

        Split s1t1 = newSplit(splitName, Status.ACTIVE, "tt");
        Split s1t2 = newSplit(splitName, Status.ACTIVE, "mytt");

        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t2), 1L);

        Assert.assertFalse(mSplitsStorage.isValidTrafficType("tt"));
        Assert.assertTrue(mSplitsStorage.isValidTrafficType("mytt"));
    }

    @Test
    public void existingChangedTrafficTypeForSplit() {
        String splitName = "n_s1";

        Split s0 = newSplit("n_s0", Status.ACTIVE, "tt");
        Split s1t1 = newSplit(splitName, Status.ACTIVE, "tt");
        Split s1t2 = newSplit(splitName, Status.ACTIVE, "mytt");

        mSplitsStorage.update(Arrays.asList(s0), 1L);
        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t1), 1L);
        mSplitsStorage.update(Arrays.asList(s1t2), 1L);

        Assert.assertTrue(mSplitsStorage.isValidTrafficType("tt"));
        Assert.assertTrue(mSplitsStorage.isValidTrafficType("mytt"));
    }

    private Split newSplit(String name, Status status, String trafficType) {
        Split split = new Split();
        split.name = name;
        split.status = status;
        if (trafficType != null) {
            split.trafficTypeName = trafficType;
        } else {
            split.trafficTypeName = "custom";
        }
        return split;
    }

}

