package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.api.EventMetadata;

public class EventMetadataHelpersTest {

    // Tests for createUpdatedFlagsMetadata (existing)
    @Test
    public void createUpdatedFlagsMetadataContainsFlags() {
        List<String> flags = Arrays.asList("flag1", "flag2", "flag3");
        EventMetadata metadata = EventMetadataHelpers.createUpdatedFlagsMetadata(flags);

        assertTrue(metadata.containsKey("updatedFlags"));
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) metadata.get("updatedFlags");
        assertEquals(3, result.size());
        assertTrue(result.contains("flag1"));
        assertTrue(result.contains("flag2"));
        assertTrue(result.contains("flag3"));
    }

    // Tests for createCacheReadyMetadata
    @Test
    public void createCacheReadyMetadataWithTimestampAndFreshInstallFalse() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(1234567890L, false);

        assertEquals(1234567890L, metadata.get("lastUpdateTimestamp"));
        assertEquals(false, metadata.get("freshInstall"));
    }

    @Test
    public void createCacheReadyMetadataWithNullTimestampAndFreshInstallTrue() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertNull(metadata.get("lastUpdateTimestamp"));
        assertEquals(true, metadata.get("freshInstall"));
    }

    @Test
    public void createCacheReadyMetadataKeysAreCorrect() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(123L, false);

        assertTrue(metadata.containsKey("lastUpdateTimestamp"));
        assertTrue(metadata.containsKey("freshInstall"));
        assertEquals(2, metadata.keys().size());
    }

    @Test
    public void createCacheReadyMetadataWithZeroTimestamp() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(0L, false);

        assertEquals(0L, metadata.get("lastUpdateTimestamp"));
        assertEquals(false, metadata.get("freshInstall"));
    }

    @Test
    public void createCacheReadyMetadataForCachePath() {
        // Cache path: freshInstall=false, timestamp from storage
        long storedTimestamp = 1700000000000L;
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(storedTimestamp, false);

        assertFalse((Boolean) metadata.get("freshInstall"));
        assertEquals(storedTimestamp, metadata.get("lastUpdateTimestamp"));
    }

    @Test
    public void createCacheReadyMetadataForSyncPath() {
        // Sync path: freshInstall=true, timestamp=null
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertTrue((Boolean) metadata.get("freshInstall"));
        assertNull(metadata.get("lastUpdateTimestamp"));
    }
}

