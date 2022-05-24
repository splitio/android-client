package io.split.android.client.storage.db.impressions.unique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.utils.Json;
import tests.database.GenericDaoTest;

public class UniqueKeysDaoTest extends GenericDaoTest {

    @Test
    public void insert() {
        HashSet<String> featureList1 = new HashSet<>();
        featureList1.add("split_1");
        featureList1.add("split_2");

        HashSet<String> featureList2 = new HashSet<>();
        featureList2.add("split_1");
        featureList2.add("split_3");

        String featureJson1 = Json.toJson(featureList1);
        String featureJson2 = Json.toJson(featureList2);
        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key",
                        featureJson1,
                        152050000,
                        0)
        );

        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key",
                        featureJson2,
                        152050000,
                        0)
        );

        List<UniqueKeyEntity> all = mRoomDb.uniqueKeysDao().getAll();

        assertEquals(2, all.size());
        UniqueKeyEntity firstEntity = all.get(0);
        assertEquals(firstEntity.getFeatureList(), featureJson1);
        assertEquals(firstEntity.getStatus(), 0);
        assertEquals(firstEntity.getUserKey(), "user_key");
        UniqueKeyEntity secondEntity = all.get(1);
        assertEquals(secondEntity.getFeatureList(), featureJson2);
        assertEquals(secondEntity.getStatus(), 0);
        assertEquals(secondEntity.getUserKey(), "user_key");
    }

    @Test
    public void insertMultiple() {
        HashSet<String> featureList1 = new HashSet<>();
        featureList1.add("split_1");
        featureList1.add("split_2");

        HashSet<String> featureList2 = new HashSet<>();
        featureList2.add("split_1");
        featureList2.add("split_3");

        String featureJson1 = Json.toJson(featureList1);
        String featureJson2 = Json.toJson(featureList2);
        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key",
                        featureJson1,
                        152050040,
                        0)
        );

        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key_2",
                        featureJson2,
                        152050000,
                        0)
        );

        List<UniqueKeyEntity> all = mRoomDb.uniqueKeysDao().getAll();

        assertEquals(2, all.size());
        UniqueKeyEntity firstEntity = all.get(0);
        UniqueKeyEntity secondEntity = all.get(1);
        assertEquals(firstEntity.getFeatureList(), featureJson1);
        assertEquals(firstEntity.getStatus(), 0);
        assertEquals(firstEntity.getUserKey(), "user_key");
        assertEquals(secondEntity.getFeatureList(), featureJson2);
        assertEquals(secondEntity.getStatus(), 0);
        assertEquals(secondEntity.getUserKey(), "user_key_2");
    }

    @Test
    public void getBy() {
        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key",
                        "split1",
                        152050000,
                        0)
        );

        mRoomDb.uniqueKeysDao().insert(
                new UniqueKeyEntity("user_key_2",
                        "split1",
                        152050020,
                        0)
        );

        List<UniqueKeyEntity> firstQuery = mRoomDb.uniqueKeysDao().getBy(162050000, 0, 100);
        List<UniqueKeyEntity> secondQuery = mRoomDb.uniqueKeysDao().getBy(152050000, 0, 100);
        List<UniqueKeyEntity> thirdQuery = mRoomDb.uniqueKeysDao().getBy(152050010, 0, 100);

        assertEquals(0, firstQuery.size());
        assertEquals(2, secondQuery.size());
        assertEquals(1, thirdQuery.size());
    }

    @Test
    public void updateStatus() {
        List<Long> ids = new ArrayList<>();
        for (String s : Arrays.asList("user_key", "user_key_1", "user_key_2")) {
            ids.add(mRoomDb.uniqueKeysDao().insert(
                    new UniqueKeyEntity(s, "split1", 152050000, 0)
            ));
        }

        mRoomDb.uniqueKeysDao().updateStatus(Arrays.asList(ids.get(0), ids.get(1)), 1);
        List<UniqueKeyEntity> allEntities = mRoomDb.uniqueKeysDao().getAll();
        assertEquals(1, allEntities.get(0).getStatus());
        assertEquals(1, allEntities.get(1).getStatus());
        assertEquals(0, allEntities.get(2).getStatus());
    }

    @Test
    public void delete() {
        for (String s : Arrays.asList("user_key", "user_key_1", "user_key_2")) {
            mRoomDb.uniqueKeysDao().insert(
                    new UniqueKeyEntity(s, "split1", 152050000, 0)
            );
        }

        mRoomDb.uniqueKeysDao().delete(Arrays.asList("user_key", "user_key_1"));

        List<UniqueKeyEntity> allEntities = mRoomDb.uniqueKeysDao().getAll();
        assertEquals(1, allEntities.size());
        assertEquals("user_key_2", allEntities.get(0).getUserKey());
    }

    @Test
    public void deleteOutdated() {
        int i = 1;
        for (String s : Arrays.asList("user_key", "user_key_1", "user_key_2")) {
            mRoomDb.uniqueKeysDao().insert(
                    new UniqueKeyEntity(s, "split1", Long.parseLong("1520" + i + "0000"), 0)
            );
            i++;
        }

        mRoomDb.uniqueKeysDao().deleteOutdated(152010001);
        List<UniqueKeyEntity> allEntities = mRoomDb.uniqueKeysDao().getAll();

        assertEquals(2, allEntities.size());

        mRoomDb.uniqueKeysDao().deleteOutdated(152030001);
        allEntities = mRoomDb.uniqueKeysDao().getAll();

        assertEquals(0, allEntities.size());
    }

    @Test
    public void deleteByStatus() {
        mRoomDb.uniqueKeysDao().insert(
            Arrays.asList(
                    new UniqueKeyEntity("key_1", "split1", 152010000, 1),
                    new UniqueKeyEntity("key_2", "split1", 152010000, 0),
                    new UniqueKeyEntity("key_3", "split1", 152010000, 1)
            )
        );

        mRoomDb.uniqueKeysDao().deleteByStatus(1, 152050000, 100);

        List<UniqueKeyEntity> allEntities = mRoomDb.uniqueKeysDao().getAll();
        assertEquals(1, allEntities.size());
    }

    @Test
    public void deleteByPrimaryKey() {
        UniqueKeyEntity key1Entity = new UniqueKeyEntity("key_1", "split1", 152010000, 1);
        UniqueKeyEntity key2Entity = new UniqueKeyEntity("key_2", "split1", 152010000, 0);
        UniqueKeyEntity key3Entity = new UniqueKeyEntity("key_3", "split1", 152010000, 1);
        mRoomDb.uniqueKeysDao().insert(Arrays.asList(key1Entity, key2Entity, key3Entity));

        mRoomDb.uniqueKeysDao().deleteById(Arrays.asList(key2Entity.getId(), key2Entity.getId()));
    }
}
