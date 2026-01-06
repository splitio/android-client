package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.SplitClient;
import io.split.android.client.events.metadata.EventMetadata;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.client.events.metadata.TypedTaskConverter;

/**
 * Tests for typed task metadata conversion in SplitEventsManager.
 */
public class TypedTaskConversionTest {

    @Test
    public void sdkUpdateEventTaskReceivesConvertedMetadata() {
        List<String> expectedFlags = Arrays.asList("flag1", "flag2");
        AtomicReference<SdkUpdateMetadata> receivedMetadata = new AtomicReference<>();

        SdkUpdateEventTask task = new SdkUpdateEventTask() {
            @Override
            public void onPostExecution(SplitClient client, SdkUpdateMetadata metadata) {
                receivedMetadata.set(metadata);
            }
        };

        EventMetadata eventMetadata = EventMetadataHelpers.createUpdatedFlagsMetadata(expectedFlags);
        SplitClient client = mock(SplitClient.class);

        // Call conversion method
        SdkUpdateMetadata converted = TypedTaskConverter.convertForSdkUpdate(eventMetadata);

        assertNotNull(converted);
        assertEquals(expectedFlags.size(), converted.getUpdatedFlags().size());
        assertTrue(converted.getUpdatedFlags().containsAll(expectedFlags));
    }

    @Test
    public void sdkReadyFromCacheEventTaskReceivesConvertedMetadata() {
        long expectedTimestamp = 1704067200000L;
        AtomicReference<SdkReadyFromCacheMetadata> receivedMetadata = new AtomicReference<>();

        SdkReadyFromCacheEventTask task = new SdkReadyFromCacheEventTask() {
            @Override
            public void onPostExecution(SplitClient client, SdkReadyFromCacheMetadata metadata) {
                receivedMetadata.set(metadata);
            }
        };

        EventMetadata eventMetadata = EventMetadataHelpers.createCacheReadyMetadata(expectedTimestamp, true);
        SplitClient client = mock(SplitClient.class);

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

