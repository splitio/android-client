package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import helper.DatabaseHelper;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsSnapshot;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;

public class PersistentSplitsStorageTest {

    final static Long INITIAL_CHANGE_NUMBER = 9999L;
    final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d}";
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentSplitsStorage mPersistentSplitsStorage;
    private final Map<String, Set<String>> mFlagSets = new HashMap<>();
    private final Map<String, Integer> mTrafficTypes = new HashMap<>();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
        List<SplitEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(JSON_SPLIT_TEMPLATE, splitName, INITIAL_CHANGE_NUMBER - i));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, INITIAL_CHANGE_NUMBER));
        mPersistentSplitsStorage = new SqLitePersistentSplitsStorage(mRoomDb,
                SplitCipherFactory.create("abcdefghijklmnopqrstuvwxyz", false));
    }

    @Test
    public void getSplits() {
        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splits = listToMap(snapshot.getSplits());

        Assert.assertNotNull(splits.get("split-0"));
        Assert.assertNotNull(splits.get("split-1"));
        Assert.assertNotNull(splits.get("split-2"));
        Assert.assertNotNull(splits.get("split-9"));
    }

    @Test
    public void getMasiveSplits() {
        List<SplitEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(JSON_SPLIT_TEMPLATE, splitName, INITIAL_CHANGE_NUMBER - i));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splits = listToMap(snapshot.getSplits());

        Assert.assertNotNull(splits.get("split-0"));
        Assert.assertNotNull(splits.get("split-1"));
        Assert.assertNotNull(splits.get("split-2"));
        Assert.assertNotNull(splits.get("split-10000"));
    }

    @Test
    public void addSplits() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String splitName = "split-test-" + i;
            Split split = new Split();
            split.name = splitName;
            split.status = Status.ACTIVE;
            splits.add(split);
        }
        mPersistentSplitsStorage.update(new ProcessedSplitChange(splits, new ArrayList<>(), 1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.getSplits());
        long changeNumber = snapshot.getChangeNumber();
        long timestamp = snapshot.getUpdateTimestamp();

        for (int i = 0; i < 10; i++) {
            String splitName = "split-test-" + i;
            Split splitTest = splitMap.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
        Assert.assertEquals(1L, changeNumber);
        Assert.assertEquals(0L, timestamp);
    }

    @Test
    public void updateEmptySplit() {
        List<Split> splits = new ArrayList<>();
        mPersistentSplitsStorage.update(new ProcessedSplitChange(splits, splits, 1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.getSplits());
        long changeNumber = snapshot.getChangeNumber();
        long timestamp = snapshot.getUpdateTimestamp();

        for (int i = 0; i < 10; i++) {
            String splitName = "split-" + i;
            Split splitTest = splitMap.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
        Assert.assertEquals(1L, changeNumber);
        Assert.assertEquals(0L, timestamp);
    }

    @Test
    public void addNullSplitList() {
        List<Split> splits = new ArrayList<>();
        boolean res = mPersistentSplitsStorage.update(new ProcessedSplitChange(null, splits,1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.getSplits());
        long changeNumber = snapshot.getChangeNumber();
        long timestamp = snapshot.getUpdateTimestamp();

        for (int i = 0; i < 10; i++) {
            String splitName = "split-" + i;
            Split splitTest = splitMap.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
        Assert.assertTrue(res);
        Assert.assertEquals(1L, changeNumber);
        Assert.assertEquals(0L, timestamp);
    }

    @Test
    public void deleteNullSplitList() {
        List<Split> splits = new ArrayList<>();
        boolean res = mPersistentSplitsStorage.update(new ProcessedSplitChange(splits, null,1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.getSplits());
        long changeNumber = snapshot.getChangeNumber();
        long timestamp = snapshot.getUpdateTimestamp();

        for (int i = 0; i < 10; i++) {
            String splitName = "split-" + i;
            Split splitTest = splitMap.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
        Assert.assertTrue(res);
        Assert.assertEquals(1L, changeNumber);
        Assert.assertEquals(0L, timestamp);
    }

    @Test
    public void deleteSplits() {
        List<Split> splits = new ArrayList<>();
        for (int i = 1; i < 10; i+=2) {
            String splitName = "split-" + i;
            Split split = new Split();
            split.name = splitName;
            split.status = Status.ARCHIVED;
            splits.add(split);
        }
        mPersistentSplitsStorage.update(new ProcessedSplitChange(null, splits, 1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.getSplits());
        long changeNumber = snapshot.getChangeNumber();
        long timestamp = snapshot.getUpdateTimestamp();

        for (int i = 0; i < 4; i++) {
            String splitName = "split-" + i;
            Split splitTest = splitMap.get(splitName);
            if(i % 2 == 0) {
                Assert.assertNotNull(splitTest);
                Assert.assertEquals(splitName, splitTest.name);
            } else {
                Assert.assertNull(splitTest);
            }
        }
        Assert.assertEquals(1L, changeNumber);
        Assert.assertEquals(0L, timestamp);
    }

    @Test
    public void initialChangeNumber() {
        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        Assert.assertEquals(INITIAL_CHANGE_NUMBER, (Long)snapshot.getChangeNumber());
    }

    @Test
    public void deleteAllSplits() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String splitName = "split-" + i;
            Split split = new Split();
            split.name = splitName;
            split.status = Status.ARCHIVED;
            splits.add(split);
        }
        mPersistentSplitsStorage.update(new ProcessedSplitChange(null, splits, 1L, 0L), mTrafficTypes, mFlagSets);

        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();
        List<Split> loadedSlits = snapshot.getSplits();
        long changeNumber = snapshot.getChangeNumber();

        Assert.assertEquals(0, loadedSlits.size());
        Assert.assertEquals(1L, changeNumber);
    }

    @Test
    public void flagsSpecValueIsSavedInDatabase() throws InterruptedException {
        final String initialFlagsSpec = mPersistentSplitsStorage.getFlagsSpec();
        final String initialDbFlagsSpec = mRoomDb.generalInfoDao().getByName("flagsSpec") == null ? null : mRoomDb.generalInfoDao().getByName("flagsSpec").getStringValue();

        mPersistentSplitsStorage.updateFlagsSpec("2.5");
        Thread.sleep(100);

        final String finalFlagsSpec = mPersistentSplitsStorage.getFlagsSpec();
        final String finalDbFlagsSpec = mRoomDb.generalInfoDao().getByName("flagsSpec") == null ? null : mRoomDb.generalInfoDao().getByName("flagsSpec").getStringValue();

        Assert.assertEquals("2.5", finalFlagsSpec);
        Assert.assertEquals("2.5", finalDbFlagsSpec);
        Assert.assertNull(initialFlagsSpec);
        Assert.assertNull(initialDbFlagsSpec);
    }

    @Test
    public void getSnapshotLoadsFlagsSpec() throws InterruptedException {
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "2.5"));
        Thread.sleep(100);
        SplitsSnapshot snapshot = mPersistentSplitsStorage.getSnapshot();

        String flagsSpec = snapshot.getFlagsSpec();

        Assert.assertEquals("2.5", flagsSpec);
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

    private Map<String, Split> listToMap(List<Split> splits) {
        Map<String, Split> map = new HashMap<>();
        for (Split split : splits) {
            map.put(split.name, split);
        }
        return map;
    }
}
