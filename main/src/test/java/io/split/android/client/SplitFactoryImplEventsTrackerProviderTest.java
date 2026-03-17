package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
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

        // Set up default behavior for traffic type validation
        when(mSplitsStorage.isValidTrafficType(anyString())).thenReturn(true);
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

    @Test
    public void trackerCallbackInvokesSyncManagerPushEvent() {
        Tracker tracker = mProvider.getEventsTracker();

        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        boolean result = tracker.track("user-key", "user", "purchase", 10.5, properties, true);

        assertTrue(result);
        verify(mSyncManager).pushEvent(any(Event.class));
    }

    @Test
    public void trackerCallbackCreatesEventWithCorrectFields() {
        Tracker tracker = mProvider.getEventsTracker();

        Map<String, Object> properties = new HashMap<>();
        properties.put("product", "widget");
        properties.put("quantity", 3);

        long beforeTrack = System.currentTimeMillis();
        tracker.track("test-key", "account", "conversion", 25.99, properties, true);
        long afterTrack = System.currentTimeMillis();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mSyncManager).pushEvent(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals("conversion", capturedEvent.eventTypeId);
        assertEquals("account", capturedEvent.trafficTypeName);
        assertEquals("test-key", capturedEvent.key);
        assertEquals(25.99, capturedEvent.value, 0.0001);
        assertTrue(capturedEvent.timestamp >= beforeTrack && capturedEvent.timestamp <= afterTrack);
        assertNotNull(capturedEvent.properties);
        assertEquals("widget", capturedEvent.properties.get("product"));
        assertEquals(3, capturedEvent.properties.get("quantity"));
        assertTrue(capturedEvent.getSizeInBytes() > 0);
    }

    @Test
    public void trackerCallbackRecordsLatencyInTelemetry() {
        Tracker tracker = mProvider.getEventsTracker();

        tracker.track("key", "user", "event", 1.0, null, true);

        ArgumentCaptor<Long> latencyCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mTelemetryStorage).recordLatency(any(Method.class), latencyCaptor.capture());

        Long latency = latencyCaptor.getValue();
        assertNotNull(latency);
        assertTrue(latency >= 0);
    }

    @Test
    public void trackerCallbackRecordsExceptionInTelemetry() {
        // Create a SyncManager that throws when pushEvent is called
        SyncManager throwingSyncManager = mock(SyncManager.class);
        doThrow(new RuntimeException("Push failed"))
                .when(throwingSyncManager).pushEvent(any(Event.class));

        SplitFactoryImpl.EventsTrackerProvider provider = new SplitFactoryImpl.EventsTrackerProvider(
                mSplitsStorage,
                mTelemetryStorage,
                throwingSyncManager);
        when(mSplitsStorage.isValidTrafficType(anyString())).thenReturn(true);

        Tracker tracker = provider.getEventsTracker();

        boolean result = tracker.track("key", "user", "event", 1.0, null, true);

        // Track should return false due to exception
        assertEquals(false, result);
        verify(mTelemetryStorage).recordException(Method.TRACK);
    }
}
