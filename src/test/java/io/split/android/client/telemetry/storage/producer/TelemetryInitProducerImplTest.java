package io.split.android.client.telemetry.storage.producer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryInitProducerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryInitProducerImpl telemetryInitProducer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryInitProducer = new TelemetryInitProducerImpl(telemetryStorage);
    }

    @Test
    public void recordNonReadyUsageCallsStorage() {

        telemetryInitProducer.recordNonReadyUsage();

        verify(telemetryStorage).recordNonReadyUsage();
    }
}
