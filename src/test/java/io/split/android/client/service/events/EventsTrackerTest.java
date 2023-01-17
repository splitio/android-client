package io.split.android.client.service.events;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.EventsTracker;
import io.split.android.client.EventsTrackerImpl;
import io.split.android.client.ProcessedEventProperties;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class EventsTrackerTest {
    @Mock
    private SplitEventsManager mEventsManager;
    @Mock
    private EventValidator mEventValidator;
    @Mock
    private ValidationMessageLogger mValidationLogger;
    @Mock
    private TelemetryStorageProducer mTelemetryStorageProducer;
    @Mock
    private EventPropertiesProcessor mEventPropertiesProcessor;
    @Mock
    private SyncManager mSyncManager;

    private EventsTracker mEventsTracker;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mEventValidator.validate(any(), anyBoolean())).thenReturn(null);
        when(mEventsManager.eventAlreadyTriggered(any())).thenReturn(true);
        when(mEventPropertiesProcessor.process(any())).thenReturn(new ProcessedEventProperties(true, null, 0));

        mEventsTracker = new EventsTrackerImpl(mEventValidator, mValidationLogger, mTelemetryStorageProducer,
                mEventPropertiesProcessor, mSyncManager);
    }

    @Test
    public void testTrackEnabled() throws InterruptedException {
        trackingEnabledTest(true);
    }

    @Test
    public void testTrackDisabled() throws InterruptedException {
        trackingEnabledTest(false);
    }

    private void trackingEnabledTest(boolean enabled) throws InterruptedException {
        mEventsTracker.enableTracking(enabled);
        boolean res = mEventsTracker.track("pepe", "tt", null, 1.0, null, true);
        Thread.sleep(500);
        assertEquals(enabled, res);
        if (enabled) {
            verify(mSyncManager, times(1)).pushEvent(any());
            verify(mTelemetryStorageProducer, times(1)).recordLatency(Method.TRACK, 0L);
        } else {
            verify(mSyncManager, never()).pushEvent(any());
            verify(mTelemetryStorageProducer, never()).recordLatency(Method.TRACK, 0L);
        }
    }

    @Test
    public void trackRecordsLatencyInEvaluationProducer() {
        ProcessedEventProperties processedEventProperties = mock(ProcessedEventProperties.class);
        when(processedEventProperties.isValid()).thenReturn(true);
        mEventsTracker.track("any", "tt", "ev", 1, null, true);

        verify(mTelemetryStorageProducer).recordLatency(eq(Method.TRACK), anyLong());
    }

    @Test
    public void trackRecordsExceptionInCaseThereIsOne() {
        when(mEventPropertiesProcessor.process(any())).thenAnswer(invocation -> {
            throw new Exception("test exception");
        });

        mEventsTracker.track("event", "tt", "ev", 0, null, true);

        verify(mTelemetryStorageProducer).recordException(Method.TRACK);
    }
}
