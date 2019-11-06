package storage;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.RoomSqLitePersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;

public class PersistentSplitsStorageTest {

    final static Long INITIAL_CHANGE_NUMBER = 9999L;
    final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d}";
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentSplitsStorage mPersistentSplitsStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
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
        mPersistentSplitsStorage = new RoomSqLitePersistentSplitsStorage(mRoomDb);
    }

    @Test
    public void getSplits() {
        Pair<List<Split>, Long> snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splits = listToMap(snapshot.first);

        Assert.assertNotNull(splits.get("split-0"));
        Assert.assertNotNull(splits.get("split-1"));
        Assert.assertNotNull(splits.get("split-2"));
        Assert.assertNotNull(splits.get("split-9"));
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
        mPersistentSplitsStorage.update(splits, 1L);

        Pair<List<Split>, Long> snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.first);
        long changeNumber = snapshot.second;

        for (int i = 0; i < 10; i++) {
            String splitName = "split-test-" + i;
            Split splitTest = splitMap.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
        Assert.assertEquals(1L, changeNumber);
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
        mPersistentSplitsStorage.update(splits, 1L);

        Pair<List<Split>, Long> snapshot = mPersistentSplitsStorage.getSnapshot();
        Map<String, Split> splitMap = listToMap(snapshot.first);
        long changeNumber = snapshot.second;

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
    }

    @Test
    public void initialChangeNumber() {
        Pair<List<Split>, Long> snapshot = mPersistentSplitsStorage.getSnapshot();
        Assert.assertEquals(INITIAL_CHANGE_NUMBER,snapshot.second);
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

