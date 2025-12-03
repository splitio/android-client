package io.split.android.client.localhost;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.storage.legacy.FileStorage;

public class LocalhostSplitsStorageTest {

    @Mock
    private Context mContext;
    @Mock
    private AssetManager mAssetManager;
    @Mock
    private FileStorage mFileStorage;
    @Mock
    private EventsManagerCoordinator mEventsManagerCoordinator;

    private LocalhostSplitsStorage mLocalhostSplitsStorage;
    private static final String TEST_FILE_NAME = "test-splits.yaml";
    private static final String INITIAL_CONTENT = "- split1:\n    treatment: \"on\"";
    private static final String UPDATED_CONTENT = "- split2:\n    treatment: \"off\"";

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(mContext.getAssets()).thenReturn(mAssetManager);
        when(mAssetManager.open(anyString())).thenThrow(new FileNotFoundException("File not found in assets"));
        when(mFileStorage.read(TEST_FILE_NAME)).thenReturn(INITIAL_CONTENT);
        mLocalhostSplitsStorage = new LocalhostSplitsStorage(TEST_FILE_NAME, mContext, mFileStorage, mEventsManagerCoordinator);
    }

    @Test
    public void loadLocalNotifiesTargetingRulesSyncCompleteAndSplitsUpdatedWhenContentChanges() throws IOException {
        // First load - should notify events (lines 219-220)
        mLocalhostSplitsStorage.loadLocal();

        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());

        // Update content and reload
        when(mFileStorage.read(TEST_FILE_NAME)).thenReturn(UPDATED_CONTENT);
        mLocalhostSplitsStorage.loadLocal();

        // Should notify events again since content changed
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(2)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(2)).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
    }

    @Test
    public void loadLocalDoesNotNotifyEventsWhenContentUnchanged() throws IOException {
        // First load - should notify events
        mLocalhostSplitsStorage.loadLocal();

        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());

        // Reload with same content - should NOT notify events again
        mLocalhostSplitsStorage.loadLocal();

        // Verify events were only called once
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(1)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(1)).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
    }

    @Test
    public void loadLocalNotifiesSplitsUpdatedWithMetadataContainingUpdatedFlags() throws IOException {
        // First load - should notify events with metadata
        mLocalhostSplitsStorage.loadLocal();

        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE), any());
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        
        ArgumentCaptor<EventMetadata> metadataCaptor = ArgumentCaptor.forClass(EventMetadata.class);
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), metadataCaptor.capture());
        
        EventMetadata metadata = metadataCaptor.getValue();
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain 'updatedFlags' key", metadata.containsKey("updatedFlags"));
        Object flagsValue = metadata.get("updatedFlags");
        assertNotNull("updatedFlags value should not be null", flagsValue);
        assertTrue("updatedFlags should be a List", flagsValue instanceof List);
        @SuppressWarnings("unchecked")
        List<String> flags = (List<String>) flagsValue;
        assertTrue("Metadata should contain 'split1' flag", flags.contains("split1"));
    }
}

