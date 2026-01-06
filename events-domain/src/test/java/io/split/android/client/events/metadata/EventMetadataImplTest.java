package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EventMetadataImplTest {

    @Test
    public void sizeAndContainsKeyReflectStoredEntries() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 42);
        data.put("key3", true);

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        assertEquals(3, metadata.size());
        assertTrue(metadata.containsKey("key1"));
        assertTrue(metadata.containsKey("key2"));
        assertTrue(metadata.containsKey("key3"));
    }

    @Test
    public void isEmptyReturnsTrueForEmptyMetadata() {
        EventMetadataImpl metadata = new EventMetadataImpl(new HashMap<>());

        assertTrue(metadata.isEmpty());
    }

    @Test
    public void valuesReturnsAllValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("string", "value");
        data.put("number", 42);

        EventMetadataImpl metadata = new EventMetadataImpl(data);
        Collection<Object> values = metadata.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("value"));
        assertTrue(values.contains(42));
    }

    @Test
    public void valuesReturnsEmptyCollectionForEmptyMetadata() {
        EventMetadataImpl metadata = new EventMetadataImpl(new HashMap<>());

        assertTrue(metadata.values().isEmpty());
    }

    @Test
    public void getReturnsValueForExistingKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        assertEquals("value", metadata.get("key"));
    }

    @Test
    public void getReturnsNullForNonExistingKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        assertNull(metadata.get("nonExistingKey"));
    }

    @Test
    public void containsKeyReturnsTrueForExistingKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        assertTrue(metadata.containsKey("key"));
    }

    @Test
    public void containsKeyReturnsFalseForNonExistingKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        assertFalse(metadata.containsKey("nonExistingKey"));
    }

    @Test
    public void metadataIsImmutableAfterConstruction() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        // Modify original map
        data.put("newKey", "newValue");

        // Metadata should not be affected
        assertFalse(metadata.containsKey("newKey"));
        assertEquals(1, metadata.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void listIsDefensivelyCopiedDuringConstruction() {
        List<String> originalList = new ArrayList<>(Arrays.asList("flag_1", "flag_2"));
        Map<String, Object> data = new HashMap<>();
        data.put("flags", originalList);

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        // Modify original list after construction
        originalList.add("flag_3");

        // Metadata should not be affected
        List<String> storedList = (List<String>) metadata.get("flags");
        assertEquals(2, storedList.size());
        assertEquals(Arrays.asList("flag_1", "flag_2"), storedList);
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings("unchecked")
    public void listReturnedByGetIsUnmodifiable() {
        Map<String, Object> data = new HashMap<>();
        data.put("flags", Arrays.asList("flag_1", "flag_2"));

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        List<String> list = (List<String>) metadata.get("flags");

        // This should throw UnsupportedOperationException
        list.add("flag_3");
    }
}
