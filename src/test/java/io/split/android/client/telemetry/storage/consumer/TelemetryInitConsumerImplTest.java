package io.split.android.client.telemetry.storage.consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryInitConsumerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryInitConsumerImpl telemetryInitConsumer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryInitConsumer = new TelemetryInitConsumerImpl(telemetryStorage);
    }

    @Test
    public void getNonReadyUsageFetchesValueFromStorage() {
        when(telemetryStorage.getNonReadyUsage()).thenReturn(25L);

        long nonReadyUsage = telemetryInitConsumer.getNonReadyUsage();

        verify(telemetryStorage).getNonReadyUsage();
        assertEquals(25L, nonReadyUsage);
    }

    @Test
    public void getBURTimeoutsFetchesValueFromStorage() {
        when(telemetryStorage.getBURTimeouts()).thenReturn(10L);

        long burTimeouts = telemetryInitConsumer.getBURTimeouts();

        verify(telemetryStorage).getBURTimeouts();
        assertEquals(10L, burTimeouts);
    }
}
