package io.split.android.client.localhost;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileNotFoundException;
import java.io.IOException;

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
    private static final String INITIAL_CONTENT = "splits:\n  - name: split1\n    treatment: on";
    private static final String UPDATED_CONTENT = "splits:\n  - name: split2\n    treatment: off";

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
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED));

        // Update content and reload
        when(mFileStorage.read(TEST_FILE_NAME)).thenReturn(UPDATED_CONTENT);
        mLocalhostSplitsStorage.loadLocal();

        // Should notify events again since content changed
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(2)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(2)).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED));
    }

    @Test
    public void loadLocalDoesNotNotifyEventsWhenContentUnchanged() throws IOException {
        // First load - should notify events
        mLocalhostSplitsStorage.loadLocal();

        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED));

        // Reload with same content - should NOT notify events again
        mLocalhostSplitsStorage.loadLocal();

        // Verify events were only called once
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(1)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManagerCoordinator, org.mockito.Mockito.times(1)).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED));
    }
}

