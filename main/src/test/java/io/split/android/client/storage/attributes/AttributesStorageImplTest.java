package io.split.android.client.storage.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Test
    public void loadFromPersistenceMergesNonDirtyKeysAndKeepsNewerInMemoryValue() {
        attributesStorage.set("A", "oldA");
        attributesStorage.set("C", "valueC");
        // simulate a newer in-memory write for A happening after the persisted snapshot was taken
        attributesStorage.set("A", "newerA");

        Map<String, Object> persisted = new HashMap<>();
        persisted.put("A", "persistedA");
        persisted.put("B", "persistedB");

        attributesStorage.loadFromPersistence(persisted);

        Map<String, Object> result = attributesStorage.getAll();
        assertEquals("newerA", result.get("A"));
        assertEquals("persistedB", result.get("B"));
        assertEquals("valueC", result.get("C"));
        assertEquals(3, result.size());
    }

    @Test
    public void loadFromPersistenceNeverOverwritesDirtyKey() {
        attributesStorage.set("key1", "inMemoryValue");

        Map<String, Object> persisted = new HashMap<>();
        persisted.put("key1", "persistedValue");

        attributesStorage.loadFromPersistence(persisted);

        assertEquals("inMemoryValue", attributesStorage.get("key1"));
    }

    @Test
    public void removeThenLoadFromPersistenceKeepsKeyAbsentTombstone() {
        attributesStorage.set("key1", "value1");
        attributesStorage.remove("key1");

        Map<String, Object> persisted = new HashMap<>();
        persisted.put("key1", "persistedValue1");

        attributesStorage.loadFromPersistence(persisted);

        assertNull(attributesStorage.get("key1"));
        assertFalse(attributesStorage.getAll().containsKey("key1"));
    }

    @Test
    public void clearThenLoadFromPersistenceDoesNotResurrectAnyKeys() {
        attributesStorage.set(getDefaultValuesMap());
        attributesStorage.clear();

        attributesStorage.loadFromPersistence(getDefaultValuesMap());

        assertEquals(0, attributesStorage.getAll().size());
    }

    @Test
    public void clearThenSetThenLoadFromPersistenceOnlyResurrectsTheReSetKey() {
        attributesStorage.set(getDefaultValuesMap());
        attributesStorage.clear();

        attributesStorage.set("key1", "reSetValue");

        Map<String, Object> persisted = new HashMap<>();
        persisted.put("key1", "persistedKey1");
        persisted.put("key2", "persistedKey2");

        attributesStorage.loadFromPersistence(persisted);

        Map<String, Object> result = attributesStorage.getAll();
        assertEquals("reSetValue", result.get("key1"));
        assertFalse(result.containsKey("key2"));
        assertEquals(1, result.size());
    }

    @Test
    public void concurrentSetsAndLoadFromPersistenceDoNotThrowAndRetainDirtyValues() throws Exception {
        int threadCount = 8;
        int iterationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < iterationsPerThread; i++) {
                    attributesStorage.set("key" + threadIndex, "value-" + threadIndex + "-" + i);
                }
            }));
        }

        futures.add(executor.submit(() -> {
            for (int i = 0; i < iterationsPerThread; i++) {
                Map<String, Object> persisted = new HashMap<>();
                for (int t = 0; t < threadCount; t++) {
                    persisted.put("key" + t, "persisted-should-not-win-" + i);
                }
                attributesStorage.loadFromPersistence(persisted);
            }
        }));

        // future.get() rethrows anything a worker threw, failing the test.
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Every key was written by the app, so no persisted value may have won.
        for (int t = 0; t < threadCount; t++) {
            Object value = attributesStorage.get("key" + t);
            assertNotNull(value);
            assertTrue(((String) value).startsWith("value-" + t + "-"));
        }
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
