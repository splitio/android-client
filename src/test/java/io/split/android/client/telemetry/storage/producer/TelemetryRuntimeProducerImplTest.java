package io.split.android.client.telemetry.storage.producer;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.AblyErrorStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryRuntimeProducerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryRuntimeProducerImpl telemetryRuntimeProducer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryRuntimeProducer = new TelemetryRuntimeProducerImpl(telemetryStorage);
    }

    @Test
    public void addTagSendsValueToStorage() {

        telemetryRuntimeProducer.addTag("tag");

        verify(telemetryStorage).addTag("tag");
    }

    @Test
    public void recordImpressionStatsRecordsInStorage() {

        telemetryRuntimeProducer.recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 2);

        verify(telemetryStorage).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 2);
    }

    @Test
    public void recordEventStatsSendsValueToStorage() {

        telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1L);

        verify(telemetryStorage).recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1L);
    }

    @Test
    public void recordSuccessfulSyncSendsValueToStorage() {

        telemetryRuntimeProducer.recordSuccessfulSync(OperationType.EVENTS, 2L);

        verify(telemetryStorage).recordSuccessfulSync(OperationType.EVENTS, 2L);
    }

    @Test
    public void recordSyncErrorSendsValueToStorage() {

        telemetryRuntimeProducer.recordSyncError(OperationType.EVENTS, 400);

        verify(telemetryStorage).recordSyncError(OperationType.EVENTS, 400);
    }

    @Test
    public void recordSyncLatencySendsValueToStorage() {

        telemetryRuntimeProducer.recordSyncLatency(OperationType.IMPRESSIONS, 300);

        verify(telemetryStorage).recordSyncLatency(OperationType.IMPRESSIONS, 300);
    }

    @Test
    public void recordAuthRejectionsSendsValueToStorage() {

        telemetryRuntimeProducer.recordAuthRejections();

        verify(telemetryStorage).recordAuthRejections();
    }

    @Test
    public void recordTokenRefreshesSendsValueToStorage() {

        telemetryRuntimeProducer.recordTokenRefreshes();

        verify(telemetryStorage).recordTokenRefreshes();
    }

    @Test
    public void recordStreamingEventsSendsValueToStorage() {

        AblyErrorStreamingEvent streamingEvent = new AblyErrorStreamingEvent(402, 1000);
        telemetryRuntimeProducer.recordStreamingEvents(streamingEvent);

        verify(telemetryStorage).recordStreamingEvents(streamingEvent);
    }

    @Test
    public void recordSessionLengthSendsValueToStorage() {

        telemetryRuntimeProducer.recordSessionLength(25000);

        verify(telemetryStorage).recordSessionLength(25000);
    }
}
