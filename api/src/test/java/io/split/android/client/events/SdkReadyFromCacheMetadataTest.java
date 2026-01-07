package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SdkReadyFromCacheMetadataTest {

    @Test
    public void isFreshInstallReturnsNullWhenConstructedWithNull() {
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(null, null);

        assertNull(metadata.isFreshInstall());
    }

    @Test
    public void isFreshInstallReturnsTrueWhenConstructedWithTrue() {
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(true, null);

        assertTrue(metadata.isFreshInstall());
    }

    @Test
    public void isFreshInstallReturnsFalseWhenConstructedWithFalse() {
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(false, null);

        assertFalse(metadata.isFreshInstall());
    }

    @Test
    public void getLastUpdateTimestampReturnsNullWhenConstructedWithNull() {
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(null, null);

        assertNull(metadata.getLastUpdateTimestamp());
    }

    @Test
    public void getLastUpdateTimestampReturnsValueWhenConstructedWithValue() {
        long timestamp = 1704067200000L;
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(null, timestamp);

        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }

    @Test
    public void bothValuesReturnCorrectlyWhenBothAreSet() {
        long timestamp = 1704067200000L;
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(true, timestamp);

        assertTrue(metadata.isFreshInstall());
        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }

    @Test
    public void bothValuesReturnCorrectlyWhenFreshInstallIsFalse() {
        long timestamp = 1704067200000L;
        SdkReadyFromCacheMetadata metadata = new SdkReadyFromCacheMetadata(false, timestamp);

        assertFalse(metadata.isFreshInstall());
        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }
}

