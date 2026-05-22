package io.split.android.client.service.sseclient.sseclient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.sseclient.spi.StreamingTelemetry;
import io.split.android.client.telemetry.model.EventTypeEnum;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.SseConnectionErrorStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetryRuntimeProducerStreamingTelemetryTest {

    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    private TelemetryRuntimeProducerStreamingTelemetry mStreamingTelemetry;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mStreamingTelemetry = new TelemetryRuntimeProducerStreamingTelemetry(mTelemetryRuntimeProducer);
    }

    @Test
    public void recordTokenSyncLatency() {
        long latencyMillis = 123L;

        mStreamingTelemetry.recordTokenSyncLatency(latencyMillis);

        verify(mTelemetryRuntimeProducer).recordSyncLatency(OperationType.TOKEN, latencyMillis);
    }

    @Test
    public void recordTokenSuccessfulSync() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordTokenSuccessfulSync(timestamp);

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(OperationType.TOKEN, timestamp);
    }

    @Test
    public void recordTokenSyncError() {
        Integer httpStatus = 500;

        mStreamingTelemetry.recordTokenSyncError(httpStatus);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.TOKEN, httpStatus);
    }

    @Test
    public void recordTokenSyncErrorWithNullStatus() {
        mStreamingTelemetry.recordTokenSyncError(null);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.TOKEN, null);
    }

    @Test
    public void recordAuthRejections() {
        mStreamingTelemetry.recordAuthRejections();

        verify(mTelemetryRuntimeProducer).recordAuthRejections();
    }

    @Test
    public void recordTokenRefreshes() {
        mStreamingTelemetry.recordTokenRefreshes();

        verify(mTelemetryRuntimeProducer).recordTokenRefreshes();
    }

    @Test
    public void recordTokenRefreshEvent() {
        long expirationTime = 9999999999L;
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordTokenRefreshEvent(expirationTime, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.TOKEN_REFRESH.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(expirationTime), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordSyncModeUpdateToStreaming() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordSyncModeUpdate(true, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.SYNC_MODE_UPDATE.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(SyncModeUpdateStreamingEvent.Mode.STREAMING.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordSyncModeUpdateToPolling() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordSyncModeUpdate(false, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.SYNC_MODE_UPDATE.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(SyncModeUpdateStreamingEvent.Mode.POLLING.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordConnectionErrorRetryable() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordConnectionError(true, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.SSE_CONNECTION_ERROR.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(SseConnectionErrorStreamingEvent.Status.REQUESTED.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordConnectionErrorNonRetryable() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordConnectionError(false, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.SSE_CONNECTION_ERROR.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(SseConnectionErrorStreamingEvent.Status.NON_REQUESTED.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordAblyError() {
        int errorCode = 40142;
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordAblyError(errorCode, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.ABLY_ERROR.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(errorCode), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordOccupancyPri() {
        int publisherCount = 5;
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordOccupancyPri(publisherCount, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.OCCUPANCY_PRI.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(publisherCount), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordOccupancySec() {
        int publisherCount = 3;
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordOccupancySec(publisherCount, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.OCCUPANCY_SEC.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(publisherCount), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordStreamingStatusEnabled() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordStreamingStatus(StreamingTelemetry.StreamingStatus.ENABLED, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(StreamingStatusStreamingEvent.Status.ENABLED.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordStreamingStatusPaused() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordStreamingStatus(StreamingTelemetry.StreamingStatus.PAUSED, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(StreamingStatusStreamingEvent.Status.PAUSED.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    public void recordStreamingStatusDisabled() {
        long timestamp = 1234567890L;

        mStreamingTelemetry.recordStreamingStatus(StreamingTelemetry.StreamingStatus.DISABLED, timestamp);

        ArgumentCaptor<StreamingEvent> eventCaptor = ArgumentCaptor.forClass(StreamingEvent.class);
        verify(mTelemetryRuntimeProducer).recordStreamingEvents(eventCaptor.capture());

        StreamingEvent event = eventCaptor.getValue();
        assertEquals(EventTypeEnum.STREAMING_STATUS.getNumericValue(), event.getEventType());
        assertEquals(Long.valueOf(StreamingStatusStreamingEvent.Status.DISABLED.getNumericValue()), event.getEventData());
        assertEquals(timestamp, event.getTimestamp());
    }
}
