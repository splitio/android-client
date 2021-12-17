package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.split.android.client.telemetry.model.Method;

public class SplitClientImplTelemetryTest extends SplitClientImplBaseTest {

    @Test
    public void trackRecordsLatencyInEvaluationProducer() {
        ProcessedEventProperties processedEventProperties = mock(ProcessedEventProperties.class);
        when(processedEventProperties.isValid()).thenReturn(true);
        when(eventPropertiesProcessor.process(null)).thenReturn(processedEventProperties);
        when(eventValidator.validate(any(), eq(false))).thenReturn(null);

        splitClient.track("any");

        verify(telemetryEvaluationProducer).recordLatency(eq(Method.TRACK), anyLong());
    }

    @Test
    public void trackRecordsExceptionInCaseThereIsOne() {
        when(eventValidator.validate(any(), anyBoolean())).thenAnswer(invocation -> {
            throw new Exception("test exception");
        });

        splitClient.track("event");

        verify(telemetryEvaluationProducer).recordException(Method.TRACK);
    }

    @Test
    public void getTreatmentRecordsException() {
        when(treatmentManager.getTreatment(anyString(), anyMap(), anyBoolean())).thenAnswer(invocation -> {
            throw new Exception("text exception");
        });

        splitClient.getTreatment("test");

        verify(telemetryEvaluationProducer).recordException(Method.TREATMENT);
    }

    @Test
    public void getTreatmentsRecordsException() {
        when(treatmentManager.getTreatments(anyList(), anyMap(), anyBoolean())).thenAnswer(invocation -> {
            throw new Exception("text exception");
        });

        splitClient.getTreatments(Arrays.asList("test", "test2"), Collections.emptyMap());

        verify(telemetryEvaluationProducer).recordException(Method.TREATMENTS);
    }

    @Test
    public void getTreatmentWithConfigRecordsException() {
        when(treatmentManager.getTreatmentWithConfig(anyString(), anyMap(), anyBoolean())).thenAnswer(invocation -> {
            throw new Exception("text exception");
        });

        splitClient.getTreatmentWithConfig("test", Collections.emptyMap());

        verify(telemetryEvaluationProducer).recordException(Method.TREATMENT_WITH_CONFIG);
    }

    @Test
    public void getTreatmentsWithConfigRecordsException() {
        when(treatmentManager.getTreatmentsWithConfig(anyList(), anyMap(), anyBoolean())).thenAnswer(invocation -> {
            throw new Exception("text exception");
        });

        splitClient.getTreatmentsWithConfig(Arrays.asList("test", "test2"), Collections.emptyMap());

        verify(telemetryEvaluationProducer).recordException(Method.TREATMENTS_WITH_CONFIG);
    }
}
