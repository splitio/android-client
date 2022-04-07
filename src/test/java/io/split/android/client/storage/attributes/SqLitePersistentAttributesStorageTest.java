package io.split.android.client.storage.attributes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;

public class SqLitePersistentAttributesStorageTest {

    @Mock
    AttributesDao attributesDao;
    private SqLitePersistentAttributesStorage storage;
    private final String matchingKey = "matching_key";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        storage = new SqLitePersistentAttributesStorage(attributesDao, "matching_key");
    }

    @Test
    public void getRetrievesValuesFromDao() {
        storage.getAll(matchingKey);

        Mockito.verify(attributesDao).getByUserKey("matching_key");
    }

    @Test
    public void clearCallsDeleteAllInAttributesDao() {
        storage.clear(matchingKey);

        Mockito.verify(attributesDao).deleteAll("matching_key");
    }

    @Test
    public void setDoesNotInteractWithValuesIfMapIsNull() {
        storage.set(matchingKey, null);

        Mockito.verifyNoInteractions(attributesDao);
    }

    @Test
    public void setSavesJsonRepresentationOfInputMap() {
        Map<String, Object> attributesMap = getExampleAttributesMap();

        storage.set(matchingKey, attributesMap);

        ArgumentCaptor<AttributesEntity> attributeCaptor = ArgumentCaptor.forClass(AttributesEntity.class);
        Mockito.verify(attributesDao).update(attributeCaptor.capture());

        Assert.assertEquals("{\"attr2\":80.05,\"attr1\":125,\"attr4\":null,\"attr3\":\"String\"}",
                attributeCaptor.getValue().getAttributes());
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
