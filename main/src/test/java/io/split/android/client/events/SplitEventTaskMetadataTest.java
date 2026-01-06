package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClient;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.metadata.EventMetadataHelpers;

public class SplitEventTaskMetadataTest {

    @Mock
    private SplitClient mClient;

    @Mock
    private EventMetadata mMetadata;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void onPostExecutionWithMetadataThrowsExceptionWhenNotImplemented() {
        SplitEventTask task = new SplitEventTask();

        assertThrows(SplitEventTaskMethodNotImplementedException.class, () -> {
            task.onPostExecution(mClient, mMetadata);
        });
    }

    @Test
    public void onPostExecutionViewWithMetadataThrowsExceptionWhenNotImplemented() {
        SplitEventTask task = new SplitEventTask();

        assertThrows(SplitEventTaskMethodNotImplementedException.class, () -> {
            task.onPostExecutionView(mClient, mMetadata);
        });
    }

    @Test
    public void onPostExecutionWithMetadataCanBeOverridden() {
        EventMetadata metadata = EventMetadataHelpers.createFlagUpdateMetadata(
                java.util.Arrays.asList("flag1", "flag2"), null);

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                // Overridden implementation
            }
        };

        // Should not throw exception
        task.onPostExecution(mClient, metadata);
    }

    @Test
    public void onPostExecutionViewWithMetadataCanBeOverridden() {
        EventMetadata metadata = EventMetadataHelpers.createFromCacheMetadata(1234567890L);

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
                // Overridden implementation
            }
        };

        // Should not throw exception
        task.onPostExecutionView(mClient, metadata);
    }

    @Test
    public void onPostExecutionWithMetadataReceivesCorrectParameters() {
        EventMetadata expectedMetadata = EventMetadataHelpers.createFlagUpdateMetadata(
                java.util.Arrays.asList("flag1", "flag2"), null);

        final boolean[] metadataReceived = {false};
        final boolean[] isFlagUpdate = {false};
        final boolean[] hasCorrectValues = {false};

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                metadataReceived[0] = metadata != null;
                isFlagUpdate[0] = metadata != null && metadata.getType() == EventMetadata.Type.FLAG_UPDATE;
                hasCorrectValues[0] = metadata != null && metadata.getValues().size() == 2;
            }
        };

        task.onPostExecution(mClient, expectedMetadata);

        assertTrue("Metadata should be received", metadataReceived[0]);
        assertTrue("Metadata should be FLAG_UPDATE type", isFlagUpdate[0]);
        assertTrue("Metadata should contain 2 flag names", hasCorrectValues[0]);
    }

    @Test
    public void onPostExecutionViewWithMetadataReceivesCorrectParameters() {
        EventMetadata expectedMetadata = EventMetadataHelpers.createFromCacheMetadata(1234567890L);

        final boolean[] metadataReceived = {false};
        final boolean[] isFromCache = {false};
        final boolean[] hasTimestamp = {false};

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
                metadataReceived[0] = metadata != null;
                isFromCache[0] = metadata != null && metadata.getType() == EventMetadata.Type.FROM_CACHE;
                hasTimestamp[0] = metadata != null && metadata.getValue() != null;
            }
        };

        task.onPostExecutionView(mClient, expectedMetadata);

        assertTrue("Metadata should be received", metadataReceived[0]);
        assertTrue("Metadata should be FROM_CACHE type", isFromCache[0]);
        assertTrue("Metadata should contain timestamp", hasTimestamp[0]);
    }

    @Test
    public void freshInstallMetadataHasCorrectType() {
        EventMetadata metadata = EventMetadataHelpers.createFreshInstallMetadata();

        assertEquals(EventMetadata.Type.FRESH_INSTALL, metadata.getType());
        assertTrue(metadata.getValues().isEmpty());
    }
}
