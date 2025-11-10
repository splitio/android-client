package io.split.android.client.storage.impressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqlitePersistentImpressionsStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;

    @Mock
    private ImpressionDao mDao;

    @Mock
    private SplitCipher mSplitCipher;

    private SqLitePersistentImpressionsStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mDatabase.impressionDao()).thenReturn(mDao);
        mStorage = new SqLitePersistentImpressionsStorage(mDatabase, 10000, mSplitCipher);
    }

    @Test
    public void impressionIsInsertedUsingDao() {
        ImpressionEntity impressionEntity = createImpressionEntity(1000, 1, "test");
        mStorage.insert(impressionEntity);

        verify(mDao).insert(impressionEntity);
    }

    @Test
    public void multipleImpressionsAreInsertedUsingDao() {
        List<ImpressionEntity> impressionEntities = new ArrayList<>();
        impressionEntities.add(createImpressionEntity(1000, 1, "test1"));
        impressionEntities.add(createImpressionEntity(1001, 1, "test2"));

        mStorage.insert(impressionEntities);

        verify(mDao).insert(impressionEntities);
    }

    @Test
    public void entityForModelUsesEncryption() {
        KeyImpression keyImpression = new KeyImpression();
        keyImpression.feature = "test_feature";
        String jsonKeyImpression = "{\"feature\":\"test_feature\"}";
        String encryptedJson = "encrypted_key_impression";

        when(mSplitCipher.encrypt(any())).thenReturn(encryptedJson);
        when(mSplitCipher.encrypt("test_feature")).thenReturn("encrypted_test_feature");
        when(mSplitCipher.decrypt(encryptedJson)).thenReturn(jsonKeyImpression);
        when(mSplitCipher.decrypt("encrypted_test_feature")).thenReturn("test_feature");

        ImpressionEntity entity = mStorage.entityForModel(keyImpression);
        assertEquals("encrypted_test_feature", entity.getTestName());
        assertEquals(encryptedJson, entity.getBody());
    }

    @Test
    public void entityForModelReturnsNullWhenEncryptionResultIsNull() {
        KeyImpression keyImpression = new KeyImpression();
        keyImpression.feature = "test_feature";

        when(mSplitCipher.encrypt(any())).thenReturn(null);

        ImpressionEntity entity = mStorage.entityForModel(keyImpression);
        assertNull(entity);
    }

    @Test
    public void deleteByStatusRemovesImpressionsUsingDaoWith100Limit() {
        mStorage.deleteByStatus(1, 1100);
        verify(mDao).deleteByStatus(1, 1100, 100);
    }

    @Test
    public void deleteOutDatedUsesDao() {
        mStorage.deleteOutdated(1000);
        verify(mDao).deleteOutdated(1000);
    }

    @Test
    public void deleteByIdUsesDao() {
        mStorage.deleteById(Collections.singletonList(1L));
        verify(mDao).delete(Collections.singletonList(1L));
    }

    @Test
    public void updateStatusUsesDao() {
        mStorage.updateStatus(Collections.singletonList(1L), 1);
        verify(mDao).updateStatus(Collections.singletonList(1L), 1);
    }

    @Test
    public void entityToModelUsesEncryption() throws JsonParseException {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setBody("encrypted_body");
        entity.setTestName("encrypted_test_name");
        entity.setId(1L);

        String decryptedBody = "{\"feature\":\"test\"}";
        String decryptedTestName = "test";
        KeyImpression expectedKeyImpression = new KeyImpression();
        expectedKeyImpression.feature = "test";
        expectedKeyImpression.storageId = 1L;

        when(mSplitCipher.decrypt("encrypted_body")).thenReturn(decryptedBody);
        when(mSplitCipher.decrypt("encrypted_test_name")).thenReturn(decryptedTestName);

        KeyImpression keyImpression = mStorage.entityToModel(entity);

        assertEquals(expectedKeyImpression.feature, keyImpression.feature);
        assertEquals(expectedKeyImpression.storageId, keyImpression.storageId);
        verify(mSplitCipher).decrypt("encrypted_body");
        verify(mSplitCipher).decrypt("encrypted_test_name");
    }

    private ImpressionEntity createImpressionEntity(long createdAt, int status, String name) {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setBody("{" + name + "}");
        entity.setTestName(name);
        entity.setCreatedAt(createdAt);
        entity.setStatus(status);
        return entity;
    }
}
