package io.split.android.client.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SdkUpdateMetadataKeysTest {

    @Test
    public void updatedFlagsKeyHasCorrectName() {
        assertEquals("updatedFlags", SdkUpdateMetadataKeys.UPDATED_FLAGS.name());
    }

    @Test
    public void updatedFlagsKeyIsNotNull() {
        assertNotNull(SdkUpdateMetadataKeys.UPDATED_FLAGS);
    }
}
