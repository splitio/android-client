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
    public void convertForSdkUpdateConvertsFlagsMetadataCorrectly() {
        List<String> expectedFlags = Arrays.asList("flag1", "flag2");

        EventMetadata eventMetadata = EventMetadataHelpers.createUpdatedFlagsMetadata(expectedFlags);

        // Call conversion method
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(eventMetadata);

        assertNotNull(converted);
        assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, converted.getType());
        assertEquals(expectedFlags.size(), converted.getNames().size());
        assertTrue(converted.getNames().containsAll(expectedFlags));
    }

    @Test
    public void convertForSdkUpdateConvertsSegmentsMetadataCorrectly() {
        List<String> expectedSegments = Arrays.asList("segment1", "segment2");

        EventMetadata eventMetadata = EventMetadataHelpers.createUpdatedSegmentsMetadata(expectedSegments);

        // Call conversion method
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(eventMetadata);

        assertNotNull(converted);
        assertEquals(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, converted.getType());
        assertEquals(expectedSegments.size(), converted.getNames().size());
        assertTrue(converted.getNames().containsAll(expectedSegments));
    }

    @Test
    public void convertForSdkReadyConvertsMetadataCorrectly() {
        long expectedTimestamp = 1704067200000L;

        EventMetadata eventMetadata = EventMetadataHelpers.createReadyMetadata(expectedTimestamp, true);

        // Call conversion method
        SdkReadyMetadata converted = TypedTaskConverter.convertForSdkReady(eventMetadata);

        assertNotNull(converted);
        assertTrue(converted.isInitialCacheLoad());
        assertEquals(Long.valueOf(expectedTimestamp), converted.getLastUpdateTimestamp());
    }

    @Test
    public void convertForSdkUpdateHandlesNullMetadata() {
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(null);

        assertNotNull(converted);
        assertNull(converted.getType());
        assertTrue(converted.getNames().isEmpty());
    }

    @Test
    public void convertForSdkReadyHandlesNullMetadata() {
        SdkReadyMetadata converted = TypedTaskConverter.convertForSdkReady(null);

        assertNotNull(converted);
        assertNull(converted.isInitialCacheLoad());
        assertNull(converted.getLastUpdateTimestamp());
    }
}
