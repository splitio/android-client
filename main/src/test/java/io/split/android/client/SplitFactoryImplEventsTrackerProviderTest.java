package io.split.android.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.tracker.Tracker;

public class SplitFactoryImplEventsTrackerProviderTest {

    private SplitsStorage mSplitsStorage;
    private TelemetryStorage mTelemetryStorage;
    private SyncManager mSyncManager;
    private SplitFactoryImpl.EventsTrackerProvider mProvider;

    @Before
    public void setUp() {
        mSplitsStorage = mock(SplitsStorage.class);
        mTelemetryStorage = mock(TelemetryStorage.class);
        mSyncManager = mock(SyncManager.class);
        mProvider = new SplitFactoryImpl.EventsTrackerProvider(
                mSplitsStorage,
                mTelemetryStorage,
                mSyncManager);
    }

    @Test
    public void getEventsTrackerReturnsNonNullTracker() {
        Tracker tracker = mProvider.getEventsTracker();

        assertNotNull(tracker);
    }

    @Test
    public void getEventsTrackerReturnsSameInstanceOnSubsequentCalls() {
        Tracker tracker1 = mProvider.getEventsTracker();
        Tracker tracker2 = mProvider.getEventsTracker();

        assertSame(tracker1, tracker2);
    }
}
