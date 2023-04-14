package io.split.android.client.storage.attributes;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;

public class SqLitePersistentAttributesStorageTest {

    @Mock
    private AttributesDao mAttributesDao;
    @Mock
    private SplitCipher mSplitCipher;
    private SqLitePersistentAttributesStorage storage;
    private final String matchingKey = "matching_key";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        storage = new SqLitePersistentAttributesStorage(mAttributesDao, mSplitCipher);
    }

    @Test
    public void getRetrievesValuesFromDao() {
        storage.getAll(matchingKey);

        Mockito.verify(mAttributesDao).getByUserKey("matching_key");
    }

    @Test
    public void clearCallsDeleteAllInAttributesDao() {
        storage.clear(matchingKey);

        Mockito.verify(mAttributesDao).deleteAll("matching_key");
    }

    @Test
    public void setDoesNotInteractWithValuesIfMapIsNull() {
        storage.set(matchingKey, null);

        Mockito.verifyNoInteractions(mAttributesDao);
    }

    @Test
    public void setSavesEncryptedJsonRepresentationOfInputMap() {
        Map<String, Object> attributesMap = getExampleAttributesMap();
        when(mSplitCipher.encrypt("{\"attr2\":80.05,\"attr1\":125,\"attr4\":null,\"attr3\":\"String\"}"))
                .thenReturn("encrypted_attributes");

        storage.set(matchingKey, attributesMap);

        ArgumentCaptor<AttributesEntity> attributeCaptor = ArgumentCaptor.forClass(AttributesEntity.class);
        Mockito.verify(mAttributesDao).update(attributeCaptor.capture());

        Assert.assertEquals("encrypted_attributes",
                attributeCaptor.getValue().getAttributes());
    }

    @Test
    public void getAllReturnsDecryptedValues() {
        AttributesEntity attributesEntity = new AttributesEntity(matchingKey, "encrypted_attributes", 0);
        when(mAttributesDao.getByUserKey(matchingKey)).thenReturn(attributesEntity);
        when(mSplitCipher.decrypt("encrypted_attributes"))
                .thenReturn("{\"attr2\":80.05,\"attr1\":125,\"attr4\":null,\"attr3\":\"String\"}");

        Map<String, Object> attributesMap = storage.getAll(matchingKey);

        Assert.assertEquals(125, attributesMap.get("attr1"));
        Assert.assertEquals(80.05d, attributesMap.get("attr2"));
        Assert.assertEquals("String", attributesMap.get("attr3"));
        Assert.assertNull(attributesMap.get("attr4"));
    }

    private Map<String, Object> getExampleAttributesMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("attr1", 125);
        map.put("attr2", 80.05f);
        map.put("attr3", "String");
        map.put("attr4", null);

        return map;
    }
}
