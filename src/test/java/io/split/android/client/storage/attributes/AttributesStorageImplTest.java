package io.split.android.client.storage.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class AttributesStorageImplTest {

    @Mock
    private PersistentAttributesStorage persistentAttributesStorage;
    private AttributesStorageImpl attributesStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributesStorage = new AttributesStorageImpl(persistentAttributesStorage);
    }

    @Test
    public void loadLocalFetchesValuesFromPersistentStorageIfPersistentStorageIsNotNull() {
        attributesStorage.loadLocal();

        Mockito.verify(persistentAttributesStorage).getAll();
    }

    @Test
    public void loadLocalDoesNotInteractWithPersistentStorageWhenItIsNull() {
        attributesStorage = new AttributesStorageImpl(null);

        attributesStorage.loadLocal();

        Mockito.verifyNoInteractions(persistentAttributesStorage);
    }

    @Test
    public void clearCallsClearInPersistentStorageIfPersistentStorageIsNotNull() {
        attributesStorage.clear();

        Mockito.verify(persistentAttributesStorage).clear();
    }

    @Test
    public void clearDoesNotCallClearInPersistentStorageWhenItIsNull() {
        attributesStorage = new AttributesStorageImpl(null);

        attributesStorage.clear();
    }

    @Test
    public void getFetchesValueFromPersistentStorage() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();

        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        assertEquals(defaultValuesMap, attributesStorage.getAll());
    }

    @Test
    public void getReturnsNullIfValueIsNotPresent() {
        Map<String, Object> defaultValuesMap = new HashMap<>();

        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        assertNull(attributesStorage.get("key1"));
    }

    @Test
    public void getAllReturnsValuesFromPersistentStorage() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();

        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        final Map<String, Object> allAttributes = attributesStorage.getAll();

        assertEquals(defaultValuesMap, allAttributes);
    }

    @Test
    public void setUpdatesValueInMemory() {
        attributesStorage.set("newKey", 200);

        assertEquals(200, attributesStorage.get("newKey"));
    }

    @Test
    public void setUpdatesValueInPersistentStorageIfPersistentStorageIsNotNull() {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("newKey", 200);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        attributesStorage.set("newKey", 200);

        Mockito.verify(persistentAttributesStorage).set(captor.capture());
        assertEquals(expectedMap, captor.getValue());
    }

    @Test
    public void setDoesNotInteractWithPersistentStorageWhenItIsNull() {
        attributesStorage = new AttributesStorageImpl(null);

        attributesStorage.set("newKey", "newValue");
    }

    @Test
    public void setWithMapUpdatesMultipleValuesInMemory() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        attributesStorage.set(defaultValuesMap);

        assertEquals(defaultValuesMap, attributesStorage.getAll());
    }

    @Test
    public void setWithMapUpdatesValuesInPersistentStorage() {
        Map<String, Object> expectedMap = getDefaultValuesMap();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        attributesStorage.set(expectedMap);

        Mockito.verify(persistentAttributesStorage).set(captor.capture());
        assertEquals(expectedMap, captor.getValue());
    }

    @Test
    public void setWithMapDoesNotInteractWithPersistentStorageWhenItIsNull() {
        attributesStorage = new AttributesStorageImpl(null);

        attributesStorage.set(getDefaultValuesMap());
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
    public void removeUpdatesValuesInPersistentStorageWhenItIsNotNull() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        attributesStorage.remove("key1");

        Mockito.verify(persistentAttributesStorage).set(captor.capture());

        Map<String, Object> capturedValue = captor.getValue();
        assertNull(capturedValue.get("key1"));
        assertEquals(defaultValuesMap.size() - 1, capturedValue.size());
    }

    @Test
    public void destroyClearsInMemoryValues() {
        attributesStorage.set(getDefaultValuesMap());

        attributesStorage.destroy();

        assertEquals(0, attributesStorage.getAll().size());
    }

    @Test
    public void destroyDoesNotClearPersistentStorageValuesIfPresent() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        attributesStorage.destroy();

        assertEquals(defaultValuesMap.size(), persistentAttributesStorage.getAll().size());
    }

    private Map<String, Object> getDefaultValuesMap() {
        HashMap<String, Object> defaultValuesMap = new HashMap<>();
        defaultValuesMap.put("key1", "value1");
        defaultValuesMap.put("key2", "value2");
        defaultValuesMap.put("key3", "value3");

        return defaultValuesMap;
    }
}
