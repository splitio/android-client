package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.events.metadata.EventMetadata;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.client.events.metadata.TypedTaskConverter;

/**
 * Tests for typed task metadata conversion.
 */
public class TypedTaskConversionTest {

    @Test
    public void convertForSdkUpdateConvertsMetadataCorrectly() {
        List<String> expectedFlags = Arrays.asList("flag1", "flag2");

        EventMetadata eventMetadata = EventMetadataHelpers.createUpdatedFlagsMetadata(expectedFlags);

        // Call conversion method
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(eventMetadata);

        assertNotNull(converted);
        assertEquals(expectedFlags.size(), converted.getUpdatedFlags().size());
        assertTrue(converted.getUpdatedFlags().containsAll(expectedFlags));
    }

    @Test
    public void convertForSdkReadyFromCacheConvertsMetadataCorrectly() {
        long expectedTimestamp = 1704067200000L;

        EventMetadata eventMetadata = EventMetadataHelpers.createCacheReadyMetadata(expectedTimestamp, true);

        // Call conversion method
        SdkReadyFromCacheMetadata converted = TypedTaskConverter.convertForSdkReadyFromCache(eventMetadata);

        assertNotNull(converted);
        assertTrue(converted.isFreshInstall());
        assertEquals(Long.valueOf(expectedTimestamp), converted.getLastUpdateTimestamp());
    }

    @Test
    public void convertForSdkUpdateHandlesNullMetadata() {
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(null);

        assertNotNull(converted);
        assertNull(converted.getUpdatedFlags());
    }

    @Test
    public void convertForSdkReadyFromCacheHandlesNullMetadata() {
        SdkReadyFromCacheMetadata converted = TypedTaskConverter.convertForSdkReadyFromCache(null);

        assertNotNull(converted);
        assertNull(converted.isFreshInstall());
        assertNull(converted.getLastUpdateTimestamp());
    }
}

