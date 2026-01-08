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
    public void createUpdatedSegmentsMetadataContainsTypeAndNames() {
        List<String> segments = Arrays.asList("segment1", "segment2");
        EventMetadata metadata = EventMetadataHelpers.createUpdatedSegmentsMetadata(segments);

        assertTrue(metadata.containsKey(MetadataKeys.TYPE));
        assertEquals(MetadataKeys.TYPE_SEGMENTS_UPDATE, metadata.get(MetadataKeys.TYPE));

        // Check names
        assertTrue(metadata.containsKey(MetadataKeys.NAMES));
        List<String> result = (List<String>) metadata.get(MetadataKeys.NAMES);
        assertEquals(2, result.size());
        assertTrue(result.contains("segment1"));
        assertTrue(result.contains("segment2"));
    }

    // Tests for createCacheReadyMetadata
    @Test
    public void createCacheReadyMetadataWithTimestampAndFreshInstallFalse() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(1234567890L, false);

        assertEquals(Long.valueOf(1234567890L), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataWithNullTimestampAndFreshInstallTrue() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.TRUE, metadata.get(MetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataKeysAreCorrect() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(123L, false);

        assertTrue(metadata.containsKey(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertTrue(metadata.containsKey(MetadataKeys.FRESH_INSTALL));
        assertEquals(2, metadata.size());
    }

    @Test
    public void createCacheReadyMetadataWithZeroTimestamp() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(0L, false);

        assertEquals(Long.valueOf(0L), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataForCachePath() {
        // Cache path: freshInstall=false, timestamp from storage
        long storedTimestamp = 1700000000000L;
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(storedTimestamp, false);

        assertEquals(Boolean.FALSE, metadata.get(MetadataKeys.FRESH_INSTALL));
        assertEquals(Long.valueOf(storedTimestamp), metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createCacheReadyMetadataForSyncPath() {
        // Sync path: freshInstall=true, timestamp=null
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertEquals(Boolean.TRUE, metadata.get(MetadataKeys.FRESH_INSTALL));
        assertNull(metadata.get(MetadataKeys.LAST_UPDATE_TIMESTAMP));
    }
}
