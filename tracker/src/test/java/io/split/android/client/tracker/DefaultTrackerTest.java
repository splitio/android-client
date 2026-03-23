package io.split.android.client.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultTrackerTest {

    @Mock
    private TrackerEventValidator mEventValidator;
    @Mock
    private TrackerLogger mTrackerLogger;
    @Mock
    private TrackerPropertyValidator mPropertyValidator;
    @Mock
    private EventPushListener mOnEventPush;
    @Mock
    private TrackLatencyListener mOnTrackLatency;
    @Mock
    private TrackExceptionListener mOnTrackException;

    private DefaultTracker mTracker;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mEventValidator.validate(anyString(), anyString(), anyString(), anyDouble(), any(), anyBoolean()))
                .thenReturn(null);
        when(mPropertyValidator.validate(any(), anyInt(), anyString()))
                .thenReturn(TrackerPropertyValidator.TrackerPropertyResult.valid(null, 0));

        mTracker = new DefaultTracker(mEventValidator, mTrackerLogger, mPropertyValidator,
                mOnEventPush, mOnTrackLatency, mOnTrackException);
    }

    @Test
    public void trackingEnabledByDefault() {
        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertTrue(result);
        verify(mOnEventPush).accept(any());
    }

    @Test
    public void trackDisabledReturnsFalse() {
        mTracker.enableTracking(false);

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertFalse(result);
        verify(mOnEventPush, never()).accept(any());
    }

    @Test
    public void trackDisabledLogsVerbose() {
        mTracker.enableTracking(false);

        mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        verify(mTrackerLogger).v("Event not tracked because tracking is disabled");
    }

    @Test
    public void validationErrorBlocksTracking() {
        when(mEventValidator.validate(anyString(), anyString(), anyString(), anyDouble(), any(), anyBoolean()))
                .thenReturn(new TrackerValidationError(true, "bad event"));

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertFalse(result);
        verify(mTrackerLogger).e(eq("bad event"), anyString());
        verify(mOnEventPush, never()).accept(any());
    }

    @Test
    public void validationWarningAllowsTracking() {
        when(mEventValidator.validate(anyString(), anyString(), anyString(), anyDouble(), any(), anyBoolean()))
                .thenReturn(new TrackerValidationError(Collections.singletonList("traffic type uppercase")));

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertTrue(result);
        verify(mTrackerLogger).log(any(TrackerValidationError.class), anyString());
        verify(mOnEventPush).accept(any());
    }

    @Test
    public void validationWarningLowercasesTrafficType() {
        when(mEventValidator.validate(anyString(), anyString(), anyString(), anyDouble(), any(), anyBoolean()))
                .thenReturn(new TrackerValidationError(Collections.singletonList("traffic type has uppercase chars")));

        mTracker.track("key", "TRAFFIC", "eventType", 1.0, null, true);

        verify(mOnEventPush).accept(argThat(event -> "traffic".equals(event.trafficType)));
    }

    @Test
    public void propertyValidationErrorBlocksTracking() {
        when(mPropertyValidator.validate(any(), anyInt(), anyString()))
                .thenReturn(TrackerPropertyValidator.TrackerPropertyResult.invalid("too large", 0));

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, new HashMap<>(), true);

        assertFalse(result);
        verify(mOnEventPush, never()).accept(any());
    }

    @Test
    public void successfulTrackInvokesOnEventPush() {
        Map<String, Object> props = new HashMap<>();
        props.put("k", "v");
        when(mPropertyValidator.validate(any(), anyInt(), anyString()))
                .thenReturn(TrackerPropertyValidator.TrackerPropertyResult.valid(props, 1024));

        boolean result = mTracker.track("key", "traffic", "eventType", 2.0, props, true);

        assertTrue(result);
        verify(mOnEventPush).accept(any());
    }

    @Test
    public void successfulTrackInvokesLatencyCallback() {
        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertTrue(result);
        verify(mOnTrackLatency).accept(any(Long.class));
    }

    @Test
    public void nullLatencyCallbackDoesNotCrash() {
        mTracker = new DefaultTracker(mEventValidator, mTrackerLogger, mPropertyValidator,
                mOnEventPush, null, null);

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertTrue(result);
        verify(mOnEventPush).accept(any());
    }

    @Test
    public void exceptionDuringTrackingInvokesOnTrackException() {
        doThrow(new RuntimeException("push failed")).when(mOnEventPush).accept(any());

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertFalse(result);
        verify(mOnTrackException).accept();
    }

    @Test
    public void nullExceptionCallbackDoesNotCrashOnException() {
        mTracker = new DefaultTracker(mEventValidator, mTrackerLogger, mPropertyValidator,
                mOnEventPush, null, null);
        doThrow(new RuntimeException("push failed")).when(mOnEventPush).accept(any());

        boolean result = mTracker.track("key", "traffic", "eventType", 1.0, null, true);

        assertFalse(result);
    }

    @Test
    public void successfulTrackPopulatesEventFieldsCorrectly() {
        Map<String, Object> props = new HashMap<>();
        props.put("k", "v");
        when(mPropertyValidator.validate(any(), anyInt(), anyString()))
                .thenReturn(TrackerPropertyValidator.TrackerPropertyResult.valid(props, 512));

        long beforeTrack = System.currentTimeMillis();
        mTracker.track("myKey", "myTraffic", "myEventType", 3.14, props, true);
        long afterTrack = System.currentTimeMillis();

        ArgumentCaptor<TrackerEvent> captor = ArgumentCaptor.forClass(TrackerEvent.class);
        verify(mOnEventPush).accept(captor.capture());

        TrackerEvent captured = captor.getValue();
        assertNotNull(captured);
        assertEquals("myKey", captured.key);
        assertEquals("myTraffic", captured.trafficType);
        assertEquals("myEventType", captured.eventType);
        assertEquals(3.14, captured.value, 0.0001);
        assertTrue(captured.timestamp >= beforeTrack && captured.timestamp <= afterTrack);
        assertEquals(512, captured.sizeInBytes);
    }

    // Helper matcher for verifying TrackerEvent fields
    private static <T> T argThat(ArgumentMatcherWithReturn<T> matcher) {
        return org.mockito.ArgumentMatchers.argThat(matcher::matches);
    }

    @FunctionalInterface
    interface ArgumentMatcherWithReturn<T> {
        boolean matches(T argument);
    }
}
