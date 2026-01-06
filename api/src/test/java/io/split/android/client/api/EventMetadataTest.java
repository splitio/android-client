package io.split.android.client.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the unified {@link EventMetadata} interface.
 * Following TDD approach - these tests define the expected behavior.
 */
public class EventMetadataTest {

    // Tests for Type enum existence and values
    @Test
    public void typeEnumContainsFlagUpdate() {
        EventMetadata.Type type = EventMetadata.Type.FLAG_UPDATE;
        assertNotNull(type);
        assertEquals("FLAG_UPDATE", type.name());
    }

    @Test
    public void typeEnumContainsSegmentUpdate() {
        EventMetadata.Type type = EventMetadata.Type.SEGMENT_UPDATE;
        assertNotNull(type);
        assertEquals("SEGMENT_UPDATE", type.name());
    }

    @Test
    public void typeEnumContainsFreshInstall() {
        EventMetadata.Type type = EventMetadata.Type.FRESH_INSTALL;
        assertNotNull(type);
        assertEquals("FRESH_INSTALL", type.name());
    }

    @Test
    public void typeEnumContainsFromCache() {
        EventMetadata.Type type = EventMetadata.Type.FROM_CACHE;
        assertNotNull(type);
        assertEquals("FROM_CACHE", type.name());
    }

    @Test
    public void typeEnumHasExactlyFourValues() {
        EventMetadata.Type[] values = EventMetadata.Type.values();
        assertEquals(4, values.length);
    }
}

