package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SdkReadyMetadataTest {

    @Test
    public void isInitialCacheLoadReturnsNullWhenConstructedWithNull() {
        SdkReadyMetadata metadata = new SdkReadyMetadata(null, null);

        assertNull(metadata.isInitialCacheLoad());
    }

    @Test
    public void isInitialCacheLoadReturnsTrueWhenConstructedWithTrue() {
        SdkReadyMetadata metadata = new SdkReadyMetadata(true, null);

        assertTrue(metadata.isInitialCacheLoad());
    }

    @Test
    public void isInitialCacheLoadReturnsFalseWhenConstructedWithFalse() {
        SdkReadyMetadata metadata = new SdkReadyMetadata(false, null);

        assertFalse(metadata.isInitialCacheLoad());
    }

    @Test
    public void getLastUpdateTimestampReturnsNullWhenConstructedWithNull() {
        SdkReadyMetadata metadata = new SdkReadyMetadata(null, null);

        assertNull(metadata.getLastUpdateTimestamp());
    }

    @Test
    public void getLastUpdateTimestampReturnsValueWhenConstructedWithValue() {
        long timestamp = 1704067200000L;
        SdkReadyMetadata metadata = new SdkReadyMetadata(null, timestamp);

        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }

    @Test
    public void bothValuesReturnCorrectlyWhenBothAreSet() {
        long timestamp = 1704067200000L;
        SdkReadyMetadata metadata = new SdkReadyMetadata(true, timestamp);

        assertTrue(metadata.isInitialCacheLoad());
        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }

    @Test
    public void bothValuesReturnCorrectlyWhenInitialCacheLoadIsFalse() {
        long timestamp = 1704067200000L;
        SdkReadyMetadata metadata = new SdkReadyMetadata(false, timestamp);

        assertFalse(metadata.isInitialCacheLoad());
        assertEquals(Long.valueOf(timestamp), metadata.getLastUpdateTimestamp());
    }
}

