package io.split.android.client.telemetry.storage.producer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryEvaluationProducerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryEvaluationProducerImpl telemetryEvaluationProducer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryEvaluationProducer = new TelemetryEvaluationProducerImpl(telemetryStorage);
    }

    @Test
    public void recordExceptionRecordsValueInStorage() {

        telemetryEvaluationProducer.recordException(Method.TRACK);

        verify(telemetryStorage).recordException(Method.TRACK);
    }

    @Test
    public void recordLatencyRecordsValueInStorage() {

        telemetryEvaluationProducer.recordLatency(Method.TREATMENT, 2000);

        verify(telemetryStorage).recordLatency(Method.TREATMENT, 2000);
    }
}
