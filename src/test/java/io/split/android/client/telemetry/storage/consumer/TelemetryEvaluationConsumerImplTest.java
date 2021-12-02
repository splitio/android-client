package io.split.android.client.telemetry.storage.consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class TelemetryEvaluationConsumerImplTest {

    @Mock
    private TelemetryStorage telemetryStorage;
    private TelemetryEvaluationConsumer telemetryEvaluationConsumer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryEvaluationConsumer = new TelemetryEvaluationConsumerImpl(telemetryStorage);
    }

    @Test
    public void popExceptionsFetchesValueFromStorage() {
        MethodExceptions mockMethodExceptions = new MethodExceptions();
        mockMethodExceptions.setTrack(1);
        when(telemetryStorage.popExceptions()).thenReturn(mockMethodExceptions);

        MethodExceptions methodExceptions = telemetryEvaluationConsumer.popExceptions();

        verify(telemetryStorage).popExceptions();
        assertEquals(1L, methodExceptions.getTrack());
    }

    @Test
    public void popLatenciesFetchesValueFromStorage() {
        List<Long> mockLatencyBucket = Arrays.asList(1L, 0L, 0L, 0L);
        MethodLatencies mockMethodLatencies = new MethodLatencies();
        mockMethodLatencies.setTrack(mockLatencyBucket);
        when(telemetryStorage.popLatencies()).thenReturn(mockMethodLatencies);

        MethodLatencies methodLatencies = telemetryEvaluationConsumer.popLatencies();

        verify(telemetryStorage).popLatencies();
        assertEquals(mockLatencyBucket, methodLatencies.getTrack());
    }
}