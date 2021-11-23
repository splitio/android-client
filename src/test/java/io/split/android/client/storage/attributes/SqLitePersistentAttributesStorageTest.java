package io.split.android.client.storage.attributes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;

public class SqLitePersistentAttributesStorageTest {

    @Mock
    AttributesDao attributesDao;
    private SqLitePersistentAttributesStorage storage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        storage = new SqLitePersistentAttributesStorage(attributesDao, "key");
    }

    @Test
    public void getRetrievesValuesFromDao() {
        storage.getAll();

        Mockito.verify(attributesDao).getByUserKey("key");
    }

    @Test
    public void clearCallsDeleteAllInAttributesDao() {
        storage.clear();

        Mockito.verify(attributesDao).deleteAll("key");
    }

    @Test
    public void setDoesNotInteractWithValuesIfMapIsNull() {
        storage.set(null);

        Mockito.verifyNoInteractions(attributesDao);
    }

    @Test
    public void setSavesJsonRepresentationOfInputMap() {
        Map<String, Object> attributesMap = getExampleAttributesMap();

        storage.set(attributesMap);

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
