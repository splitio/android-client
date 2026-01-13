package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class EventMetadataHelpersTest {

    // Tests for createUpdatedFlagsMetadata
    @Test
    @SuppressWarnings("unchecked")
    public void createUpdatedFlagsMetadataContainsTypeAndNames() {
        List<String> flags = Arrays.asList("flag1", "flag2", "flag3");
        EventMetadata metadata = EventMetadataHelpers.createUpdatedFlagsMetadata(flags);

        assertTrue(metadata.containsKey(MetadataKeys.TYPE));
        assertEquals(MetadataKeys.TYPE_FLAGS_UPDATE, metadata.get(MetadataKeys.TYPE));

        // Check names
        assertTrue(metadata.containsKey(MetadataKeys.NAMES));
        List<String> result = (List<String>) metadata.get(MetadataKeys.NAMES);
        assertEquals(3, result.size());
        assertTrue(result.contains("flag1"));
        assertTrue(result.contains("flag2"));
        assertTrue(result.contains("flag3"));
    }

    // Tests for createUpdatedSegmentsMetadata
    @Test
    @SuppressWarnings("unchecked")
    public void createUpdatedSegmentsMetadataContainsTypeAndEmptyNames() {
        EventMetadata metadata = EventMetadataHelpers.createUpdatedSegmentsMetadata();

        assertTrue(metadata.containsKey(MetadataKeys.TYPE));
        assertEquals(MetadataKeys.TYPE_SEGMENTS_UPDATE, metadata.get(MetadataKeys.TYPE));

        // Check names - should always be empty
        assertTrue(metadata.containsKey(MetadataKeys.NAMES));
        List<String> result = (List<String>) metadata.get(MetadataKeys.NAMES);
        assertTrue("Names should be empty for SEGMENTS_UPDATE", result.isEmpty());
    }

    // Tests for createReadyMetadata
    @Test
    public void createReadyMetadataWithTimestampAndInitialCacheLoadFalse() {
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(1234567890L, false);

        assertEquals(Long.valueOf(1234567890L), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
    }

    @Test
    public void createReadyMetadataWithNullTimestampAndInitialCacheLoadTrue() {
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(null, true);

        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.TRUE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
    }

    @Test
    public void createReadyMetadataKeysAreCorrect() {
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(123L, false);

        assertTrue(metadata.containsKey(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertTrue(metadata.containsKey(MetadataKeys.INITIAL_CACHE_LOAD));
        assertEquals(2, metadata.size());
    }

    @Test
    public void createReadyMetadataWithZeroTimestamp() {
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(0L, false);

        assertEquals(Long.valueOf(0L), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
    }

    @Test
    public void createReadyMetadataForCachePath() {
        // Cache path: initialCacheLoad=false, timestamp from storage
        long storedTimestamp = 1700000000000L;
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(storedTimestamp, false);

        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
        assertEquals(Long.valueOf(storedTimestamp), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createReadyMetadataForSyncPath() {
        // Sync path: initialCacheLoad=true, timestamp=null
        EventMetadata metadata = EventMetadataHelpers.createReadyMetadata(null, true);

        assertEquals(Boolean.TRUE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createSyncCompleteMetadataWhenCacheAlreadyLoaded() {
        long updateTimestamp = 1234567890L;
        EventMetadata metadata = EventMetadataHelpers.createSyncCompleteMetadata(true, updateTimestamp);

        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
        assertEquals(updateTimestamp, metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createSyncCompleteMetadataWhenCacheNotLoaded() {
        EventMetadata metadata = EventMetadataHelpers.createSyncCompleteMetadata(false, 1234567890L);

        assertEquals(Boolean.TRUE, metadata.get(MetadataKeys.INITIAL_CACHE_LOAD));
        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createSyncCompleteMetadataIgnoresTimestampWhenCacheNotLoaded() {
        // Even if a timestamp is provided, it should be ignored when cache is not loaded
        EventMetadata metadata = EventMetadataHelpers.createSyncCompleteMetadata(false, 9999999999L);

        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }
}
