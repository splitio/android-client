package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.SdkReadyFromCacheMetadataKeys;
import io.split.android.client.api.SdkUpdateMetadataKeys;

public class EventMetadataHelpersTest {

    // Tests for createUpdatedFlagsMetadata (existing)
    @Test
    public void createUpdatedFlagsMetadataContainsFlags() {
        List<String> flags = Arrays.asList("flag1", "flag2", "flag3");
        EventMetadata metadata = EventMetadataHelpers.createUpdatedFlagsMetadata(flags);

        assertTrue(metadata.containsKey(SdkUpdateMetadataKeys.UPDATED_FLAGS));
        List<String> result = metadata.get(SdkUpdateMetadataKeys.UPDATED_FLAGS);
        assertEquals(3, result.size());
        assertTrue(result.contains("flag1"));
        assertTrue(result.contains("flag2"));
        assertTrue(result.contains("flag3"));
    }

    // Tests for createCacheReadyMetadata
    @Test
    public void createCacheReadyMetadataWithTimestampAndFreshInstallFalse() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(1234567890L, false);

        assertEquals(Long.valueOf(1234567890L), metadata.get(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataWithNullTimestampAndFreshInstallTrue() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertNull(metadata.get(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.TRUE, metadata.get(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataKeysAreCorrect() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(123L, false);

        assertTrue(metadata.containsKey(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertTrue(metadata.containsKey(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
        assertEquals(2, metadata.size());
    }

    @Test
    public void createCacheReadyMetadataWithZeroTimestamp() {
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(0L, false);

        assertEquals(Long.valueOf(0L), metadata.get(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
        assertEquals(Boolean.FALSE, metadata.get(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
    }

    @Test
    public void createCacheReadyMetadataForCachePath() {
        // Cache path: freshInstall=false, timestamp from storage
        long storedTimestamp = 1700000000000L;
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(storedTimestamp, false);

        assertEquals(Boolean.FALSE, metadata.get(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
        assertEquals(Long.valueOf(storedTimestamp), metadata.get(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
    }

    @Test
    public void createCacheReadyMetadataForSyncPath() {
        // Sync path: freshInstall=true, timestamp=null
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);

        assertEquals(Boolean.TRUE, metadata.get(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL));
        assertNull(metadata.get(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP));
    }
}

