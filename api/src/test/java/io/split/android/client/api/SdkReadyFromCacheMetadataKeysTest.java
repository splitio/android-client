package io.split.android.client.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SdkReadyFromCacheMetadataKeysTest {

    @Test
    public void freshInstallKeyHasCorrectName() {
        assertEquals("freshInstall", SdkReadyFromCacheMetadataKeys.FRESH_INSTALL.name());
    }

    @Test
    public void freshInstallKeyIsNotNull() {
        assertNotNull(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL);
    }

    @Test
    public void lastUpdateTimestampKeyHasCorrectName() {
        assertEquals("lastUpdateTimestamp", SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP.name());
    }

    @Test
    public void lastUpdateTimestampKeyIsNotNull() {
        assertNotNull(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP);
    }
}
