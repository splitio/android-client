package io.split.android.client;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.fallback.FallbackConfiguration;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.FlagSetsValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;

public class TreatmentManagerTelemetryTest {

    @Mock
    Evaluator evaluator;
    @Mock
    KeyValidator keyValidator;
    @Mock
    SplitValidator splitValidator;
    @Mock
    ImpressionListener.FederatedImpressionListener impressionListener;
    @Mock
    ListenableEventsManager eventsManager;
    @Mock
    AttributesManager attributesManager;
    @Mock
    AttributesMerger attributesMerger;
    @Mock
    TelemetryStorageProducer telemetryStorageProducer;
    @Mock
    private SplitsStorage mSplitsStorage;

    private FlagSetsFilter mFlagSetsFilter;
    private TreatmentManagerImpl treatmentManager;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        mFlagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        treatmentManager = new TreatmentManagerImpl(
                "test_key",
                "test_key",
                evaluator,
                keyValidator,
                splitValidator,
                impressionListener,
                SplitClientConfig.builder().build().labelsEnabled(),
                eventsManager,
                attributesManager,
                attributesMerger,
                telemetryStorageProducer,
                mFlagSetsFilter,
                mSplitsStorage, new ValidationMessageLoggerImpl(),
                new FlagSetsValidatorImpl(),
                new PropertyValidatorImpl(),
                new FallbackTreatmentsCalculatorImpl(FallbackConfiguration.builder().build()));

        when(evaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
    }

    @After
    public void tearDown() {
        try {
            mAutoCloseable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getTreatmentRecordsLatencyInTelemetry() {

        treatmentManager.getTreatment("split", new HashMap<>(), null, false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENT), anyLong());
    }

    @Test
    public void getTreatmentsRecordsLatencyInTelemetry() {

        treatmentManager.getTreatments(Arrays.asList("split"), new HashMap<>(), null, false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENTS), anyLong());
    }

    @Test
    public void getTreatmentWithConfigRecordsLatencyInTelemetry() {
        treatmentManager.getTreatmentWithConfig("split", new HashMap<>(), null, false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENT_WITH_CONFIG), anyLong());
    }

    @Test
    public void getTreatmentsWithConfigRecordsLatencyInTelemetry() {
        treatmentManager.getTreatmentsWithConfig(Arrays.asList("split"), new HashMap<>(), null, false);

        verify(telemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_WITH_CONFIG), anyLong());
    }

    @Test
    public void nonReadyUsagesAreRecordedInProducer() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(false);

        treatmentManager.getTreatment("test", Collections.emptyMap(), null, false);

        verify(telemetryStorageProducer).recordNonReadyUsage();
    }

    @Test
    public void getTreatmentRecordsException() {
        when(keyValidator.validate(anyString(), anyString())).thenThrow(new RuntimeException("test"));

        treatmentManager.getTreatment("test", Collections.emptyMap(), null, false);

        verify(telemetryStorageProducer).recordException(Method.TREATMENT);
    }

    @Test
    public void getTreatmentsRecordsException() {
        when(keyValidator.validate(anyString(), anyString())).thenThrow(new RuntimeException("test"));

        treatmentManager.getTreatments(Arrays.asList("test", "test2"), Collections.emptyMap(), null, false);

        verify(telemetryStorageProducer).recordException(Method.TREATMENTS);
    }

    @Test
    public void getTreatmentWithConfigRecordsException() {
        when(keyValidator.validate(anyString(), anyString())).thenThrow(new RuntimeException("test"));

        treatmentManager.getTreatmentWithConfig("test", Collections.emptyMap(), null, false);

        verify(telemetryStorageProducer).recordException(Method.TREATMENT_WITH_CONFIG);
    }

    @Test
    public void getTreatmentsWithConfigRecordsException() {
        when(keyValidator.validate(anyString(), anyString())).thenThrow(new RuntimeException("test"));

        treatmentManager.getTreatmentsWithConfig(Arrays.asList("test", "test2"), Collections.emptyMap(), null, false);

        verify(telemetryStorageProducer).recordException(Method.TREATMENTS_WITH_CONFIG);
    }
}
