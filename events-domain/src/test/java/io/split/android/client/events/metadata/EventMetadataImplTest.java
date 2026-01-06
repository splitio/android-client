package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.split.android.client.api.EventMetadata;

/**
 * Tests for the unified {@link EventMetadataImpl}.
 */
public class EventMetadataImplTest {

    // Tests for FLAG_UPDATE type
    @Test
    public void flagUpdateMetadataReturnsCorrectType() {
        List<String> flagNames = Arrays.asList("flag1", "flag2");
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                flagNames,
                12345L
        );

        assertEquals(EventMetadata.Type.FLAG_UPDATE, metadata.getType());
    }

    @Test
    public void flagUpdateMetadataReturnsCorrectValues() {
        List<String> flagNames = Arrays.asList("flag1", "flag2", "flag3");
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                flagNames,
                null
        );

        List<String> values = metadata.getValues();
        assertEquals(3, values.size());
        assertTrue(values.contains("flag1"));
        assertTrue(values.contains("flag2"));
        assertTrue(values.contains("flag3"));
    }

    @Test
    public void flagUpdateMetadataReturnsChangeNumber() {
        List<String> flagNames = Arrays.asList("flag1");
        Long changeNumber = 999L;
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                flagNames,
                changeNumber
        );

        assertEquals(changeNumber, metadata.getValue());
    }

    @Test
    public void flagUpdateMetadataWithNullChangeNumber() {
        List<String> flagNames = Arrays.asList("flag1");
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                flagNames,
                null
        );

        assertNull(metadata.getValue());
    }

    // Tests for SEGMENT_UPDATE type
    @Test
    public void segmentUpdateMetadataReturnsCorrectType() {
        List<String> segmentNames = Arrays.asList("segment1", "segment2");
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.SEGMENT_UPDATE,
                segmentNames,
                null
        );

        assertEquals(EventMetadata.Type.SEGMENT_UPDATE, metadata.getType());
    }

    @Test
    public void segmentUpdateMetadataReturnsCorrectValues() {
        List<String> segmentNames = Arrays.asList("segment1", "segment2");
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.SEGMENT_UPDATE,
                segmentNames,
                null
        );

        List<String> values = metadata.getValues();
        assertEquals(2, values.size());
        assertTrue(values.contains("segment1"));
        assertTrue(values.contains("segment2"));
    }

    // Tests for FRESH_INSTALL type
    @Test
    public void freshInstallMetadataReturnsCorrectType() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FRESH_INSTALL,
                Collections.emptyList(),
                null
        );

        assertEquals(EventMetadata.Type.FRESH_INSTALL, metadata.getType());
    }

    @Test
    public void freshInstallMetadataReturnsEmptyValues() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FRESH_INSTALL,
                Collections.emptyList(),
                null
        );

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    public void freshInstallMetadataReturnsNullValue() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FRESH_INSTALL,
                Collections.emptyList(),
                null
        );

        assertNull(metadata.getValue());
    }

    // Tests for FROM_CACHE type
    @Test
    public void fromCacheMetadataReturnsCorrectType() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FROM_CACHE,
                Collections.emptyList(),
                1700000000000L
        );

        assertEquals(EventMetadata.Type.FROM_CACHE, metadata.getType());
    }

    @Test
    public void fromCacheMetadataReturnsEmptyValues() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FROM_CACHE,
                Collections.emptyList(),
                1700000000000L
        );

        assertTrue(metadata.getValues().isEmpty());
    }

    @Test
    public void fromCacheMetadataReturnsLastUpdateTimestamp() {
        long timestamp = 1700000000000L;
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FROM_CACHE,
                Collections.emptyList(),
                timestamp
        );

        assertEquals(Long.valueOf(timestamp), metadata.getValue());
    }

    // Immutability tests
    @Test
    public void valuesListIsImmutable() {
        List<String> original = new ArrayList<>(Arrays.asList("flag1", "flag2"));
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                original,
                null
        );

        // Modify original list after construction
        original.add("flag3");

        // Metadata should not be affected
        assertEquals(2, metadata.getValues().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void valuesListReturnedIsUnmodifiable() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                Arrays.asList("flag1", "flag2"),
                null
        );

        // This should throw UnsupportedOperationException
        metadata.getValues().add("flag3");
    }

    @Test
    public void getValuesNeverReturnsNull() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FRESH_INSTALL,
                Collections.emptyList(),
                null
        );

        assertNotNull(metadata.getValues());
    }

    @Test
    public void getTypeNeverReturnsNull() {
        EventMetadata metadata = new EventMetadataImpl(
                EventMetadata.Type.FLAG_UPDATE,
                Collections.emptyList(),
                null
        );

        assertNotNull(metadata.getType());
    }
}

