package io.split.android.client.events;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClient;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.SdkReadyFromCacheMetadataKeys;
import io.split.android.client.api.SdkUpdateMetadataKeys;
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
        EventMetadata metadata = EventMetadataHelpers.createUpdatedFlagsMetadata(
                java.util.Arrays.asList("flag1", "flag2"));

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
        EventMetadata metadata = EventMetadataHelpers.createCacheReadyMetadata(1234567890L, false);

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
        EventMetadata expectedMetadata = EventMetadataHelpers.createUpdatedFlagsMetadata(
                java.util.Arrays.asList("flag1", "flag2"));

        final boolean[] metadataReceived = {false};
        final boolean[] hasUpdatedFlags = {false};

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client, EventMetadata metadata) {
                metadataReceived[0] = metadata != null;
                hasUpdatedFlags[0] = metadata != null && metadata.containsKey(SdkUpdateMetadataKeys.UPDATED_FLAGS);
            }
        };

        task.onPostExecution(mClient, expectedMetadata);

        assertTrue("Metadata should be received", metadataReceived[0]);
        assertTrue("Metadata should contain updatedFlags", hasUpdatedFlags[0]);
    }

    @Test
    public void onPostExecutionViewWithMetadataReceivesCorrectParameters() {
        EventMetadata expectedMetadata = EventMetadataHelpers.createCacheReadyMetadata(1234567890L, false);

        final boolean[] metadataReceived = {false};
        final boolean[] hasTimestamp = {false};
        final boolean[] hasFreshInstall = {false};

        SplitEventTask task = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
                metadataReceived[0] = metadata != null;
                hasTimestamp[0] = metadata != null && metadata.containsKey(SdkReadyFromCacheMetadataKeys.LAST_UPDATE_TIMESTAMP);
                hasFreshInstall[0] = metadata != null && metadata.containsKey(SdkReadyFromCacheMetadataKeys.FRESH_INSTALL);
            }
        };

        task.onPostExecutionView(mClient, expectedMetadata);

        assertTrue("Metadata should be received", metadataReceived[0]);
        assertTrue("Metadata should contain lastUpdateTimestamp", hasTimestamp[0]);
        assertTrue("Metadata should contain freshInstall", hasFreshInstall[0]);
    }
}

