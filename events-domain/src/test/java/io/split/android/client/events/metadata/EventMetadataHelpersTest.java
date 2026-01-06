package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.split.android.client.api.EventMetadata;

/**
 * Tests for the {@link EventMetadataHelpers} factory methods.
 */
public class EventMetadataHelpersTest {

    // Tests for createFlagUpdateMetadata
    @Test
    public void createFlagUpdateMetadataReturnsCorrectType() {
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                Arrays.asList("flag1", "flag2"),
                null
        );

        assertEquals(EventMetadata.Type.FLAG_UPDATE, metadata.getType());
    }

    @Test
    public void createFlagUpdateMetadataReturnsCorrectFlagNames() {
        List<String> flagNames = Arrays.asList("flag1", "flag2", "flag3");
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(flagNames, null);

        List<String> values = metadata.getValues();
        assertEquals(3, values.size());
        assertTrue(values.contains("flag1"));
        assertTrue(values.contains("flag2"));
        assertTrue(values.contains("flag3"));
    }

    @Test
    public void createFlagUpdateMetadataReturnsChangeNumber() {
        Long changeNumber = 12345L;
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                Arrays.asList("flag1"),
                changeNumber
        );

        assertEquals(changeNumber, metadata.getValue());
    }

    @Test
    public void createFlagUpdateMetadataWithNullChangeNumber() {
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                Arrays.asList("flag1"),
                null
        );

        assertNull(metadata.getValue());
    }

    @Test
    public void createFlagUpdateMetadataDeduplicatesFlagNames() {
        List<String> flagNames = Arrays.asList("flag1", "flag2", "flag1", "flag3", "flag2");
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(flagNames, null);

        List<String> values = metadata.getValues();
        // Should have deduplicated - only 3 unique flags
        assertEquals(3, values.size());
    }

    // Tests for createSegmentUpdateMetadata
    @Test
    public void createSegmentUpdateMetadataReturnsCorrectType() {
        EventMetadata metadata = EventMetadataHelpers.createSegmentUpdateMetadata(
                Arrays.asList("segment1", "segment2"),
                null
        );

        assertEquals(EventMetadata.Type.SEGMENT_UPDATE, metadata.getType());
    }

    @Test
    public void createSegmentUpdateMetadataReturnsCorrectSegmentNames() {
        List<String> segmentNames = Arrays.asList("segment1", "segment2");
        EventMetadata metadata = EventMetadataHelpers.createSegmentUpdateMetadata(segmentNames, null);

        List<String> values = metadata.getValues();
        assertEquals(2, values.size());
        assertTrue(values.contains("segment1"));
        assertTrue(values.contains("segment2"));
    }

    @Test
    public void createSegmentUpdateMetadataReturnsChangeNumber() {
        Long changeNumber = 67890L;
        EventMetadata metadata = EventMetadataHelpers.createSegmentUpdateMetadata(
                Arrays.asList("segment1"),
                changeNumber
        );

        assertEquals(changeNumber, metadata.getValue());
    }

    // Tests for createFreshInstallMetadata
    @Test
    public void createFreshInstallMetadataReturnsCorrectType() {
        EventMetadata metadata = EventMetadataHelpers.createFreshInstallMetadata();

        assertEquals(EventMetadata.Type.FRESH_INSTALL, metadata.getType());
    }

    @Test
    public void createFreshInstallMetadataReturnsEmptyValues() {
        EventMetadata metadata = EventMetadataHelpers.createFreshInstallMetadata();

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    public void createFreshInstallMetadataReturnsNullValue() {
        EventMetadata metadata = EventMetadataHelpers.createFreshInstallMetadata();

        assertNull(metadata.getValue());
    }

    // Tests for createFromCacheMetadata
    @Test
    public void createFromCacheMetadataReturnsCorrectType() {
        EventMetadata metadata = EventMetadataHelpers.createFromCacheMetadata(1700000000000L);

        assertEquals(EventMetadata.Type.FROM_CACHE, metadata.getType());
    }

    @Test
    public void createFromCacheMetadataReturnsEmptyValues() {
        EventMetadata metadata = EventMetadataHelpers.createFromCacheMetadata(1700000000000L);

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    public void createFromCacheMetadataReturnsLastUpdateTimestamp() {
        long timestamp = 1700000000000L;
        EventMetadata metadata = EventMetadataHelpers.createFromCacheMetadata(timestamp);

        assertEquals(Long.valueOf(timestamp), metadata.getValue());
    }

    @Test
    public void createFromCacheMetadataWithZeroTimestamp() {
        EventMetadata metadata = EventMetadataHelpers.createFromCacheMetadata(0L);

        assertEquals(Long.valueOf(0L), metadata.getValue());
    }
}

