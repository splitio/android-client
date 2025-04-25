package tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.utils.Json;

public class SplitsStorageTest {

    private static final Long INITIAL_CHANGE_NUMBER = 9999L;
    private static final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d}";

    private SplitRoomDatabase mRoomDb;
    private SplitsStorage mSplitsStorage;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(context);
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
        mSplitsStorage = new SplitsStorageImpl(new SqLitePersistentSplitsStorage(mRoomDb, SplitCipherFactory.create("abcdefghijklmnopqrstuvwxyz", false)));
    }

    @Test
    public void noLocalLoaded() {
        Map<String, Split> all = mSplitsStorage.getAll();

        Assert.assertEquals(0, all.size());
    }

    @Test
    public void getSplits() {
        mSplitsStorage.loadLocal();
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
        mSplitsStorage.loadLocal();
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String splitName = "split-test-" + i;
            Split split = new Split();
            split.name = splitName;
            split.status = Status.ACTIVE;
            splits.add(split);
        }
        ProcessedSplitChange change = new ProcessedSplitChange(splits, new ArrayList<>(), 1L, 0L);
        mSplitsStorage.update(change);

        for (int i = 0; i < 4; i++) {
            String splitName = "split-test-" + i;
            Split splitTest = mSplitsStorage.get(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
    }

    @Test
    public void initialChangeNumber() {
        mSplitsStorage.loadLocal();
        Assert.assertEquals((long) INITIAL_CHANGE_NUMBER, mSplitsStorage.getTill());
    }

    @Test
    public void updateChangeNumber() {
        mSplitsStorage.loadLocal();
        List<Split> splits = new ArrayList<>();
        Long newChangeNumber = INITIAL_CHANGE_NUMBER + 100;
        Long initialChangeNumber = mSplitsStorage.getTill();
        ProcessedSplitChange change = new ProcessedSplitChange(splits, splits, newChangeNumber, 0L);
        mSplitsStorage.update(change);
        Long updatedChangeNumber = mSplitsStorage.getTill();
        Assert.assertEquals(INITIAL_CHANGE_NUMBER, initialChangeNumber);
        Assert.assertEquals(newChangeNumber, updatedChangeNumber);
    }

    @Test
    public void updateEmptySplit() {
        mSplitsStorage.loadLocal();
        List<Split> splits = new ArrayList<>();
        ProcessedSplitChange change = new ProcessedSplitChange(splits, splits, 1L, 0L);
        mSplitsStorage.update(change);

        Map<String, Split> loadedSplits = mSplitsStorage.getMany(null);
        long changeNumber = mSplitsStorage.getTill();

        Assert.assertEquals(4, loadedSplits.size());
        Assert.assertEquals(1L, changeNumber);
    }

    @Test
    public void addNullSplitList() {
        mSplitsStorage.loadLocal();
        ProcessedSplitChange change = new ProcessedSplitChange(null, new ArrayList<>(), 1L, 0L);
        mSplitsStorage.update(change);

        Map<String, Split> loadedSplits = mSplitsStorage.getMany(null);
        long changeNumber = mSplitsStorage.getTill();

        Assert.assertEquals(4, loadedSplits.size());
        Assert.assertEquals(1L, changeNumber);
    }

    @Test
    public void deleteNullSplitList() {
        mSplitsStorage.loadLocal();
        ProcessedSplitChange change = new ProcessedSplitChange(new ArrayList<>(), null, 1L, 0L);
        mSplitsStorage.update(change);

        Map<String, Split> loadedSplits = mSplitsStorage.getMany(null);
        long changeNumber = mSplitsStorage.getTill();

        Assert.assertEquals(4, loadedSplits.size());
        Assert.assertEquals(1L, changeNumber);
    }

    @Test
    public void getManyNullArgs() {
        mSplitsStorage.loadLocal();
        Map<String, Split> loadedSplits = mSplitsStorage.getMany(null);

        Assert.assertEquals(4, loadedSplits.size());
    }

    @Test
    public void getManyEmptyArgs() {
        mSplitsStorage.loadLocal();
        Map<String, Split> loadedSplits = mSplitsStorage.getMany(new ArrayList<>());

        Assert.assertEquals(4, loadedSplits.size());
    }

    @Test
    public void getMany() {
        mSplitsStorage.loadLocal();
        Map<String, Split> loadedSplits = mSplitsStorage.getMany(Arrays.asList("split-0", "split-3", "non-existing"));

        Assert.assertEquals(2, loadedSplits.size());
        Assert.assertNotNull(loadedSplits.get("split-0"));
        Assert.assertNotNull(loadedSplits.get("split-3"));
    }

    @Test
    public void updateToStorageConcurrency() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int j = 1000; j < 1200; j += 10) {
                    List<Split> activeSplits = new ArrayList<>();
                    List<Split> archivedSplits = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        int p = j + i;
                        String splitName = "split-test-" + p;
                        Split split = new Split();
                        split.name = splitName;
                        split.status = (p % 2 == 0 ? Status.ACTIVE : Status.ARCHIVED);
                        if (split.status == Status.ACTIVE) {
                            activeSplits.add(split);
                        } else {
                            archivedSplits.add(split);
                        }
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                        }
                    }
                    ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, 1L, 0L);
                    mSplitsStorage.update(change);
                }
                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int j = 0; j < 200; j += 10) {
                    List<Split> activeSplits = new ArrayList<>();
                    List<Split> archivedSplits = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        int p = j + i;
                        String splitName = "split-test-" + p;
                        Split split = new Split();
                        split.name = splitName;
                        split.status = (p % 2 != 0 ? Status.ACTIVE : Status.ARCHIVED);
                        if (split.status == Status.ACTIVE) {
                            activeSplits.add(split);
                        } else {
                            archivedSplits.add(split);
                        }
                        ;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, 1L, 0L);
                    mSplitsStorage.update(change);
                }
                latch.countDown();
            }
        }).start();
        latch.await(40, TimeUnit.SECONDS);

        Assert.assertNotNull(mSplitsStorage.get("split-test-1000"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-1100"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-1198"));
        assertNull(mSplitsStorage.get("split-test-1001"));
        assertNull(mSplitsStorage.get("split-test-1101"));
        assertNull(mSplitsStorage.get("split-test-1199"));

        Assert.assertNotNull(mSplitsStorage.get("split-test-1"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-101"));
        Assert.assertNotNull(mSplitsStorage.get("split-test-199"));
        assertNull(mSplitsStorage.get("split-test-0"));
        assertNull(mSplitsStorage.get("split-test-100"));
        assertNull(mSplitsStorage.get("split-test-198"));

    }

    @Test
    public void updatedSplitTrafficType() {
        mSplitsStorage.loadLocal();
        List<Split> empty = new ArrayList<>();
        Split s1 = newSplit("s1", Status.ACTIVE, "tt");

        Split s2 = newSplit("s2", Status.ACTIVE, "mytt");
        Split s2ar = newSplit("s2", Status.ARCHIVED, "mytt");

        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s2), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s2), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s2), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(empty, Arrays.asList(s2ar), 1L, 0L));

        assertTrue(mSplitsStorage.isValidTrafficType("tt"));
        assertFalse(mSplitsStorage.isValidTrafficType("mytt"));
    }

    @Test
    public void changedTrafficTypeForSplit() {
        mSplitsStorage.loadLocal();
        List<Split> empty = new ArrayList<>();
        String splitName = "n_s1";

        Split s1t1 = newSplit(splitName, Status.ACTIVE, "tt");
        Split s1t2 = newSplit(splitName, Status.ACTIVE, "mytt");

        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t2), empty, 1L, 0L));

        assertFalse(mSplitsStorage.isValidTrafficType("tt"));
        assertTrue(mSplitsStorage.isValidTrafficType("mytt"));
    }

    @Test
    public void existingChangedTrafficTypeForSplit() {
        mSplitsStorage.loadLocal();
        List<Split> empty = new ArrayList<>();
        String splitName = "n_s1";

        Split s0 = newSplit("n_s0", Status.ACTIVE, "tt");
        Split s1t1 = newSplit(splitName, Status.ACTIVE, "tt");
        Split s1t2 = newSplit(splitName, Status.ACTIVE, "mytt");

        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s0), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t1), empty, 1L, 0L));
        mSplitsStorage.update(new ProcessedSplitChange(Arrays.asList(s1t2), empty, 1L, 0L));

        assertTrue(mSplitsStorage.isValidTrafficType("tt"));
        assertTrue(mSplitsStorage.isValidTrafficType("mytt"));
    }

    @Test
    public void trafficTypesAreLoadedInMemoryWhenLoadingLocalSplits() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type"), newSplitEntity("split_test_2", "test_type_2")));

        Map<String, Integer> trafficTypes = new HashMap<>();
        trafficTypes.put("test_type", 1);
        trafficTypes.put("test_type_2", 1);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, Json.toJson(trafficTypes)));

        mSplitsStorage.loadLocal();

        assertTrue(mSplitsStorage.isValidTrafficType("test_type"));
        assertTrue(mSplitsStorage.isValidTrafficType("test_type_2"));
        assertFalse(mSplitsStorage.isValidTrafficType("invalid_type"));
    }

    @Test
    public void loadedFromStorageTrafficTypesAreCorrectlyUpdated() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type"), newSplitEntity("split_test_2", "test_type_2")));

        Map<String, Integer> trafficTypes = new HashMap<>();
        trafficTypes.put("test_type", 1);
        trafficTypes.put("test_type_2", 1);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, Json.toJson(trafficTypes)));

        mSplitsStorage.loadLocal();

        Split updatedSplit = newSplit("split_test", Status.ACTIVE, "new_type");
        mSplitsStorage.update(new ProcessedSplitChange(Collections.singletonList(updatedSplit), Collections.emptyList(), 1L, 0L));

        assertFalse(mSplitsStorage.isValidTrafficType("test_type"));
        assertTrue(mSplitsStorage.isValidTrafficType("new_type"));
        assertTrue(mSplitsStorage.isValidTrafficType("test_type_2"));
    }

    @Test
    public void flagSetsAreUpdatedWhenCallingLoadLocal() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(
                newSplitEntity("split_test", "test_type", Collections.singleton("set_1")),
                newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2")),
                newSplitEntity("split_test_3", "test_type_2", Collections.singleton("set_2")),
                newSplitEntity("split_test_4", "test_type_2", Collections.singleton("set_1"))));

        Map<String, Set<String>> flagSets = new HashMap<>();
        flagSets.put("set_1", new HashSet<>(Arrays.asList("split_test", "split_test_4")));
        flagSets.put("set_2", new HashSet<>(Arrays.asList("split_test_2", "split_test_3")));

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, Json.toJson(flagSets)));

        mSplitsStorage.loadLocal();

        Assert.assertEquals(new HashSet<>(Arrays.asList("split_test", "split_test_4")), mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")));
        Assert.assertEquals(new HashSet<>(Arrays.asList("split_test_2", "split_test_3")), mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_2")));
    }

    @Test
    public void flagSetsAreRemovedWhenUpdating() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(
                newSplitEntity("split_test", "test_type", Collections.singleton("set_1")),
                newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2")),
                newSplitEntity("split_test_3", "test_type_2", Collections.singleton("set_2"))));

        Map<String, Set<String>> flagSets = new HashMap<>();
        flagSets.put("set_1", new HashSet<>(Arrays.asList("split_test")));
        flagSets.put("set_2", new HashSet<>(Arrays.asList("split_test_2", "split_test_3")));

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, Json.toJson(flagSets)));

        mSplitsStorage.loadLocal();

        Set<String> initialSet1 = mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"));
        Set<String> initialSet2 = mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_2"));

        mSplitsStorage.update(new ProcessedSplitChange(
                Collections.singletonList(newSplit("split_test", Status.ACTIVE, "test_type")), Collections.emptyList(),
                1L, 0L));

        assertFalse(initialSet1.isEmpty());
        Assert.assertEquals(Collections.emptySet(), mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")));
        Assert.assertEquals(initialSet2, mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_2")));
    }

    @Test
    public void updateWithoutChecksRemovesFromFlagSet() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type", Collections.singleton("set_1")), newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2"))));

        Map<String, Set<String>> flagSets = new HashMap<>();
        flagSets.put("set_1", new HashSet<>(Arrays.asList("split_test")));
        flagSets.put("set_2", new HashSet<>(Arrays.asList("split_test_2")));

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, Json.toJson(flagSets)));

        mSplitsStorage.loadLocal();

        Set<String> initialSet1 = mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"));
        Set<String> initialSet2 = mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_2"));

        mSplitsStorage.updateWithoutChecks(newSplit("split_test", Status.ACTIVE, "test_type"));

        assertFalse(initialSet1.isEmpty());
        Assert.assertEquals(Collections.emptySet(), mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")));
        Assert.assertEquals(initialSet2, mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_2")));
    }

    @Test
    public void updateReturnsTrueWhenFlagsHaveBeenRemovedFromStorage() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type", Collections.singleton("set_1")), newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2"))));
        mSplitsStorage.loadLocal();

        ArrayList<Split> archivedSplits = new ArrayList<>();
        archivedSplits.add(newSplit("split_test", Status.ARCHIVED, "test_type"));
        boolean update = mSplitsStorage.update(new ProcessedSplitChange(new ArrayList<>(), archivedSplits, 1L, 0L));

        assertTrue(update);
    }

    @Test
    public void updateReturnsTrueWhenFlagsWereAddedToStorage() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type", Collections.singleton("set_1")), newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2"))));
        mSplitsStorage.loadLocal();

        ArrayList<Split> activeSplits = new ArrayList<>();
        activeSplits.add(newSplit("split_test_3", Status.ACTIVE, "test_type_2", Collections.singleton("set_2")));
        boolean update = mSplitsStorage.update(new ProcessedSplitChange(activeSplits, new ArrayList<>(), 1L, 0L));

        assertTrue(update);
    }

    @Test
    public void updateReturnsTrueWhenFlagsWereUpdatedInStorage() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type", Collections.singleton("set_1")), newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2"))));
        mSplitsStorage.loadLocal();

        ArrayList<Split> activeSplits = new ArrayList<>();
        activeSplits.add(newSplit("split_test", Status.ACTIVE, "test_type", Collections.singleton("set_2")));
        boolean update = mSplitsStorage.update(new ProcessedSplitChange(activeSplits, new ArrayList<>(), 1L, 0L));

        assertTrue(update);
    }

    @Test
    public void updateReturnsFalseWhenFlagsThatAreNotInStorageAreAttemptedToBeRemoved() {
        mRoomDb.clearAllTables();
        mRoomDb.splitDao().insert(Arrays.asList(newSplitEntity("split_test", "test_type", Collections.singleton("set_1")), newSplitEntity("split_test_2", "test_type_2", Collections.singleton("set_2"))));
        mSplitsStorage.loadLocal();

        ArrayList<Split> archivedSplits = new ArrayList<>();
        archivedSplits.add(newSplit("split_test_3", Status.ACTIVE, "test_type_2", Collections.singleton("set_2")));
        boolean update = mSplitsStorage.update(new ProcessedSplitChange(new ArrayList<>(), archivedSplits, 1L, 0L));

        assertFalse(update);
    }

    @Test
    public void loadLocalLoadsFlagsSpecValue() {
        mRoomDb.clearAllTables();
        String initialFlagsSpec = mSplitsStorage.getFlagsSpec();

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "2.5"));

        mSplitsStorage.loadLocal();

        String finalFlagsSpec = mSplitsStorage.getFlagsSpec();

        assertNull(initialFlagsSpec);
        assertEquals("2.5", finalFlagsSpec);
    }

    @Test
    public void updateFlagsSpecValuePersistsValueInDatabase() {
        mRoomDb.clearAllTables();
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "2.0"));
        mSplitsStorage.loadLocal();
        String initialFlagsSpec = mSplitsStorage.getFlagsSpec();

        mSplitsStorage.updateFlagsSpec("2.5");

        String dbFlagsSpec = mRoomDb.generalInfoDao().getByName("flagsSpec").getStringValue();
        String finalFlagsSpec = mSplitsStorage.getFlagsSpec();

        assertEquals("2.0", initialFlagsSpec);
        assertEquals("2.5", finalFlagsSpec);
        assertEquals("2.5", dbFlagsSpec);
    }

    @Test
    public void nullFlagsSpecValueIsValid() {
        mRoomDb.clearAllTables();
        mSplitsStorage.loadLocal();

        String flagsSpec = mSplitsStorage.getFlagsSpec();

        assertEquals("", flagsSpec);
    }

    private Split newSplit(String name, Status status, String trafficType) {
        return newSplit(name, status, trafficType, Collections.emptySet());
    }

    private Split newSplit(String name, Status status, String trafficType, Set<String> sets) {
        Split split = new Split();
        split.name = name;
        split.status = status;
        if (trafficType != null) {
            split.trafficTypeName = trafficType;
        } else {
            split.trafficTypeName = "custom";
        }
        split.sets = sets;

        return split;
    }

    private static SplitEntity newSplitEntity(String name, String trafficType) {
        return newSplitEntity(name, trafficType, Collections.emptySet());
    }

    private static SplitEntity newSplitEntity(String name, String trafficType, Set<String> sets) {
        SplitEntity entity = new SplitEntity();
        String setsString = String.join(",", sets);
        entity.setName(name);
        entity.setBody(String.format(IntegrationHelper.JSON_SPLIT_WITH_TRAFFIC_TYPE_TEMPLATE, name, INITIAL_CHANGE_NUMBER, trafficType, setsString));

        return entity;
    }
}
