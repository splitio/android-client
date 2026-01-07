package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link MetadataKeys}.
 * Verifies that all metadata keys are correctly defined.
 */
public class MetadataKeysTest {

    @Test
    public void updatedFlagsKeyHasCorrectValue() {
        assertEquals("updatedFlags", MetadataKeys.UPDATED_FLAGS);
    }

    @Test
    public void freshInstallKeyHasCorrectValue() {
        assertEquals("freshInstall", MetadataKeys.FRESH_INSTALL);
    }

    @Test
    public void lastUpdateTimestampKeyHasCorrectValue() {
        assertEquals("lastUpdateTimestamp", MetadataKeys.LAST_UPDATE_TIMESTAMP);
    }
}
