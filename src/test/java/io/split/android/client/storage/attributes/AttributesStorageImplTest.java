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
        attributesStorage = new AttributesStorageImpl(persistentAttributesStorage, true);
    }

    @Test
    public void loadLocalFetchesValuesFromPersistentStorage() {
        attributesStorage.loadLocal();

        Mockito.verify(persistentAttributesStorage).getAll();
    }

    @Test
    public void clearCallsClearInPersistentStorage() {
        attributesStorage.clear();

        Mockito.verify(persistentAttributesStorage).clear();
    }

    @Test
    public void getFetchesCorrespondingValuePreviouslyLoadedFromPersistentStorage() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();

        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        assertEquals("value1", attributesStorage.get("key1"));
    }

    @Test
    public void getReturnsNullIfValueIsNotPresent() {
        Map<String, Object> defaultValuesMap = new HashMap<>();

        Mockito.when(persistentAttributesStorage.getAll()).thenReturn(defaultValuesMap);

        attributesStorage.loadLocal();

        assertNull(attributesStorage.get("key1"));
    }

    @Test
    public void getAllReturnsAllValuesPreviouslyRetrievedFromPersistentStorage() {
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
    public void setUpdatesValueInPersistentStorage() {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("newKey", 200);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        attributesStorage.set("newKey", 200);

        Mockito.verify(persistentAttributesStorage).set(captor.capture());
        assertEquals(expectedMap, captor.getValue());
    }

    @Test
    public void setDoesNotUpdateValuesIfCacheIsDisabled() {
        attributesStorage = new AttributesStorageImpl(persistentAttributesStorage, false);

        attributesStorage.set("newKey", "newValue");

        Mockito.verifyNoInteractions(persistentAttributesStorage);
    }

    @Test
    public void setWithMapUpdatesMultipleValuesInMemory() {
        Map<String, Object> defaultValuesMap = getDefaultValuesMap();
        attributesStorage.set(defaultValuesMap);

        assertEquals(defaultValuesMap, attributesStorage.getAll());
    }

    @Test
    public void setWithMapUpdatesMultipleValuesInPersistentStorage() {
        Map<String, Object> expectedMap = getDefaultValuesMap();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        attributesStorage.set(expectedMap);

        Mockito.verify(persistentAttributesStorage).set(captor.capture());
        assertEquals(expectedMap, captor.getValue());
    }

    @Test
    public void setWithMapDoesNotUpdateIfCacheIsDisabled() {
        attributesStorage = new AttributesStorageImpl(persistentAttributesStorage, false);

        attributesStorage.set(getDefaultValuesMap());

        Mockito.verifyNoInteractions(persistentAttributesStorage);
    }

    @Test
    public void setCacheEnabledAllowsPersistentStorageToBeUsed() {
        Map<String, Object> map = new HashMap<>();
        map.put("newKey", "newValue");
        attributesStorage.setCacheEnabled(true);

        attributesStorage.set("newKey", "newValue");

        Mockito.verify(persistentAttributesStorage).set(map);
    }

    @Test
    public void setCacheDisabledDisallowsPersistentStorageToBeUsed() {
        attributesStorage.setCacheEnabled(false);

        attributesStorage.set("newKey", "newValue");

        Mockito.verifyNoInteractions(persistentAttributesStorage);
    }

    private Map<String, Object> getDefaultValuesMap() {
        HashMap<String, Object> defaultValuesMap = new HashMap<>();
        defaultValuesMap.put("key1", "value1");
        defaultValuesMap.put("key2", "value2");
        defaultValuesMap.put("key3", "value3");

        return defaultValuesMap;
    }
}
