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
        storage.get();

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

        Assert.assertEquals("{\"attr2\":8000,\"attr1\":{\"id\":10010,\"property1\":\"some_property\",\"property2\":[\"val1\",\"val2\",\"val3\",\"val4\"]},\"attr4\":null,\"attr3\":\"String\"}",
                attributeCaptor.getValue().getAttributes());
    }

    private Map<String, Object> getExampleAttributesMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("attr1", new ArbitraryAttributeType());
        map.put("attr2", 8000L);
        map.put("attr3", "String");
        map.put("attr4", null);

        return map;
    }

    private static class ArbitraryAttributeType {

        private final int id;
        private final String property1;
        private final List<String> property2;

        ArbitraryAttributeType() {
            ArrayList<String> list = new ArrayList<>();
            list.add("val1");
            list.add("val2");
            list.add("val3");
            list.add("val4");

            id = 10010;
            property1 = "some_property";
            property2 = list;
        }

        public int getId() {
            return id;
        }
    }
}
