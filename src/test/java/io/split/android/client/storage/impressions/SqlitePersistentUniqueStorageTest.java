package io.split.android.client.storage.impressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.service.impressions.unique.UniqueKey;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;

public class SqlitePersistentUniqueStorageTest {

    private SqlitePersistentUniqueStorage mStorage;

    @Mock
    private SplitRoomDatabase mDatabase;
    @Mock
    private UniqueKeysDao mDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mDatabase.uniqueKeysDao()).thenReturn(mDao);
        mStorage = new SqlitePersistentUniqueStorage(mDatabase, 3600);
    }

    @Test
    public void deleteByIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        mStorage.deleteById(ids);

        verify(mDao).deleteById(ids);
    }

    @Test
    public void deleteByStatus() {
        mStorage.deleteByStatus(1, 200L);

        verify(mDao).deleteByStatus(eq(1), eq(200L), anyInt());
    }

    @Test
    public void deleteOutdated() {
        mStorage.deleteOutdated(2000L);

        verify(mDao).deleteOutdated(2000L);
    }

    @Test
    public void insert() {
        UniqueKeyEntity entity = new UniqueKeyEntity("key", "splits", 200L, 0);
        mStorage.insert(entity);

        verify(mDao).insert(entity);
    }

    @Test
    public void multiInsert() {
        UniqueKeyEntity entity1 = new UniqueKeyEntity("key1", "splits", 200L, 0);
        UniqueKeyEntity entity2 = new UniqueKeyEntity("key2", "splits", 200L, 0);
        List<UniqueKeyEntity> entities = Arrays.asList(entity1, entity2);
        mStorage.insert(entities);

        verify(mDao).insert(entities);
    }

    @Test
    public void updateStatus() {
        List<Long> ids = Arrays.asList(25L, 26L);
        mStorage.updateStatus(ids, 1);

        verify(mDao).updateStatus(ids, 1);
    }

    @Test
    public void modelToEntity() {
        Set<String> features = getBasicFeatures();
        UniqueKey model = new UniqueKey("key", features);
        UniqueKeyEntity uniqueKeyEntity = mStorage.entityForModel(model);

        assertEquals("key", uniqueKeyEntity.getUserKey());
        assertEquals(0, uniqueKeyEntity.getId());
        assertEquals("[\"split_1\",\"split_2\"]", uniqueKeyEntity.getFeatureList());
        assertEquals(0, uniqueKeyEntity.getStatus());
        assertTrue(uniqueKeyEntity.getCreatedAt() > 0);
    }

    @Test
    public void entityToModel() {
        UniqueKeyEntity entity = new UniqueKeyEntity("key", "[\"split_1\",\"split_2\"]", 200L, 0);
        UniqueKey model = mStorage.entityToModel(entity);

        assertEquals("key", model.getKey());
        assertTrue(model.getFeatures().contains("split_1"));
        assertTrue(model.getFeatures().contains("split_2"));
        assertEquals(2, model.getFeatures().size());
        assertEquals(entity.getId(), model.getId());
    }

    @Test
    public void runInTransaction() {
        UniqueKeyEntity entity1 = new UniqueKeyEntity("key1", "splits", 200L, 0);
        UniqueKeyEntity entity2 = new UniqueKeyEntity("key2", "splits", 200L, 0);
        List<UniqueKeyEntity> entities = Arrays.asList(entity1, entity2);
        mStorage.runInTransaction(entities, 3, 3600);

        verify(mDatabase).runInTransaction(argThat((ArgumentMatcher<Runnable>) argument -> argument instanceof SqlitePersistentUniqueStorage.GetAndUpdate));
    }

    @NonNull
    private Set<String> getBasicFeatures() {
        Set<String> features = new HashSet<>();
        features.add("split_1");
        features.add("split_2");
        return features;
    }
}
