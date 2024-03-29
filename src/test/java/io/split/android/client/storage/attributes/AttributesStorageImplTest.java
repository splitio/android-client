package io.split.android.client.storage.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class AttributesStorageImplTest {

    private AttributesStorageImpl attributesStorage;
    private HashMap<String, Object> defaultValuesMap = null;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributesStorage = new AttributesStorageImpl();
    }

    @Test
    public void setUpdatesValueInMemory() {
        attributesStorage.set("newKey", 200);

        assertEquals(200, attributesStorage.get("newKey"));
    }

    @Test
    public void setNewValueRetainsPreviousValues() {
        attributesStorage.set(getDefaultValuesMap());

        attributesStorage.set("newKey", "newValue");

        assertTrue(attributesStorage.getAll().entrySet().containsAll(getDefaultValuesMap().entrySet()));
        assertTrue(attributesStorage.getAll().containsKey("newKey"));
    }

    @Test
    public void setMultipleNewValuesRetainsPreviousValues() {
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("newKey", "newValue");
        newValues.put("newKey2", "newValue2");
        newValues.put("key1", "newValue1");

        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("newKey", "newValue");
        expectedValues.put("newKey2", "newValue2");
        expectedValues.put("key1", "newValue1");
        expectedValues.put("key2", "value2");
        expectedValues.put("key3", "value3");

        attributesStorage.set(getDefaultValuesMap());
        attributesStorage.set(newValues);

        Map<String, Object> entries = attributesStorage.getAll();
        assertEquals(5, entries.size());
        assertEquals(expectedValues, entries);
    }

    @Test
    public void setWithMapUpdatesMultipleValuesInMemory() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        attributesStorage.set(defaultValuesMap);

        assertEquals(defaultValuesMap, attributesStorage.getAll());
    }

    @Test
    public void removeRemovesKeyFromMemoryStorage() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        attributesStorage.set(defaultValuesMap);

        attributesStorage.remove("key1");

        assertNull(attributesStorage.get("key1"));
        assertEquals(defaultValuesMap.size() - 1, attributesStorage.getAll().size());
    }

    @Test
    public void clearRemovesAllValuesFromMemory() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        attributesStorage.set(defaultValuesMap);
        assertEquals(defaultValuesMap.size(), attributesStorage.getAll().size());

        attributesStorage.clear();

        assertEquals(0, attributesStorage.getAll().size());
    }

    @Test
    public void destroyClearsInMemoryValues() {
        attributesStorage.set(getDefaultValuesMap());

        attributesStorage.destroy();

        assertEquals(0, attributesStorage.getAll().size());
    }

    private Map<String, Object> getDefaultValuesMap() {
        if (defaultValuesMap == null) {
            defaultValuesMap = new HashMap<>();
            defaultValuesMap.put("key1", "value1");
            defaultValuesMap.put("key2", "value2");
            defaultValuesMap.put("key3", "value3");
        }

        return defaultValuesMap;
    }
}
