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
import java.util.Set;

public class EventMetadataImplTest {

    @Test
    public void keysReturnsAllKeys() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 42);
        data.put("key3", true);

        EventMetadataImpl metadata = new EventMetadataImpl(data);
        Set<String> keys = metadata.keys();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    public void keysReturnsEmptySetForEmptyMetadata() {
        EventMetadataImpl metadata = new EventMetadataImpl(new HashMap<>());

        assertTrue(metadata.keys().isEmpty());
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

    // endregion

    // region get() tests

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

    // endregion

    // region toMap() tests

    @Test
    public void toMapReturnsACopyOfTheData() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        EventMetadataImpl metadata = new EventMetadataImpl(data);
        Map<String, Object> copy = metadata.toMap();

        assertEquals(1, copy.size());
        assertEquals("value", copy.get("key"));

        // Verify it's a copy by modifying it
        copy.put("newKey", "newValue");
        assertFalse(metadata.containsKey("newKey"));
    }

    @Test
    public void toMapReturnsEmptyMapForEmptyMetadata() {
        EventMetadataImpl metadata = new EventMetadataImpl(new HashMap<>());

        assertTrue(metadata.toMap().isEmpty());
    }

    @Test
    public void toMapReturnsModifiableCopyOfLists() {
        Map<String, Object> data = new HashMap<>();
        data.put("flags", Arrays.asList("flag_1", "flag_2"));

        EventMetadataImpl metadata = new EventMetadataImpl(data);
        Map<String, Object> copy = metadata.toMap();

        // Should be able to modify the list in the copy
        @SuppressWarnings("unchecked")
        List<String> listInCopy = (List<String>) copy.get("flags");
        listInCopy.add("flag_3");

        // Original metadata should not be affected
        @SuppressWarnings("unchecked")
        List<String> originalList = (List<String>) metadata.get("flags");
        assertEquals(2, originalList.size());
        assertEquals(Arrays.asList("flag_1", "flag_2"), originalList);
    }

    @Test
    public void toMapListsAreIndependentAcrossCalls() {
        Map<String, Object> data = new HashMap<>();
        data.put("flags", Arrays.asList("flag_1", "flag_2"));

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        Map<String, Object> copy1 = metadata.toMap();
        Map<String, Object> copy2 = metadata.toMap();

        // Modify copy1's list
        @SuppressWarnings("unchecked")
        List<String> list1 = (List<String>) copy1.get("flags");
        list1.add("flag_3");

        // copy2's list should not be affected
        @SuppressWarnings("unchecked")
        List<String> list2 = (List<String>) copy2.get("flags");
        assertEquals(2, list2.size());
        assertEquals(Arrays.asList("flag_1", "flag_2"), list2);
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
        assertEquals(1, metadata.keys().size());
    }

    @Test
    public void listIsDefensivelyCopiedDuringConstruction() {
        List<String> originalList = new ArrayList<>(Arrays.asList("flag_1", "flag_2"));
        Map<String, Object> data = new HashMap<>();
        data.put("flags", originalList);

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        // Modify original list after construction
        originalList.add("flag_3");

        // Metadata should not be affected
        @SuppressWarnings("unchecked")
        List<String> storedList = (List<String>) metadata.get("flags");
        assertEquals(2, storedList.size());
        assertEquals(Arrays.asList("flag_1", "flag_2"), storedList);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void listReturnedByGetIsUnmodifiable() {
        Map<String, Object> data = new HashMap<>();
        data.put("flags", Arrays.asList("flag_1", "flag_2"));

        EventMetadataImpl metadata = new EventMetadataImpl(data);

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) metadata.get("flags");

        // This should throw UnsupportedOperationException
        list.add("flag_3");
    }
}
