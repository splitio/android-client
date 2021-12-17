package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryEvaluationProducer;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;

public class TreatmentManagerTelemetryTest {

    @Mock
    Evaluator evaluator;
    @Mock
    KeyValidator keyValidator;
    @Mock
    SplitValidator splitValidator;
    @Mock
    ImpressionListener impressionListener;
    @Mock
    ISplitEventsManager eventsManager;
    @Mock
    AttributesManager attributesManager;
    @Mock
    AttributesMerger attributesMerger;
    @Mock
    TelemetryStorageProducer telemetryStorageProducer;

    private TreatmentManagerImpl treatmentManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        treatmentManager = new TreatmentManagerImpl(
                "test_key",
                "test_key",
                evaluator,
                keyValidator,
                splitValidator,
                impressionListener,
                SplitClientConfig.builder().build(),
                eventsManager,
                attributesManager,
                attributesMerger,
                telemetryStorageProducer
        );

        when(evaluator.getTreatment(anyString(), anyString(), anyString(), anyMap(), any())).thenReturn(new EvaluationResult("test", "label"));
    }

    @Test
    public void getTreatmentRecordsLatencyInTelemetry() {

        treatmentManager.getTreatment("split", new HashMap<>(), false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENT), anyLong());
    }

    @Test
    public void getTreatmentsRecordsLatencyInTelemetry() {

        treatmentManager.getTreatments(Arrays.asList("split"), new HashMap<>(), false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENTS), anyLong());
    }

    @Test
    public void getTreatmentWithConfigRecordsLatencyInTelemetry() {
        treatmentManager.getTreatmentWithConfig("split", new HashMap<>(), false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENT_WITH_CONFIG), anyLong());
    }

    @Test
    public void getTreatmentsWithConfigRecordsLatencyInTelemetry() {
        treatmentManager.getTreatmentsWithConfig(Arrays.asList("split"), new HashMap<>(), false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_WITH_CONFIG), anyLong());
    }

    @Test
    public void nonReadyUsagesAreRecordedInProducer() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(false);

        treatmentManager.getTreatment("test", Collections.emptyMap(), false);

        verify(telemetryStorageProducer).recordNonReadyUsage();
    }
}
