package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.PropertyValidatorImpl;
import io.split.android.client.validators.SplitFilterValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;

public class TreatmentManagerExceptionsTest {

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
    @Mock
    private SplitFilterValidator mFlagSetsValidator;

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
                mSplitsStorage,
                new ValidationMessageLoggerImpl(),
                mFlagSetsValidator,
                new PropertyValidatorImpl());

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
    public void getTreatmentLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenThrow(new RuntimeException("test"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);

        treatmentManager.getTreatment("test", Collections.emptyMap(), null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(1)).log(argumentCaptor.capture());
        assertEquals("exception", argumentCaptor.getValue().appliedRule());
        assertEquals("control", argumentCaptor.getValue().treatment());
    }

    @Test
    public void getTreatmentsLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);

        Map<String, String> treatments = treatmentManager.getTreatments(Arrays.asList("test", "test2"), Collections.emptyMap(), null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(0).appliedRule());
        assertEquals("control", allValues.get(0).treatment());
        assertEquals("default", allValues.get(1).appliedRule());
        assertEquals("on", allValues.get(1).treatment());
        assertEquals(2, treatments.size());
        assertEquals("control", treatments.get("test"));
        assertEquals("on", treatments.get("test2"));
    }

    @Test
    public void getTreatmentWithConfigLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);

        treatmentManager.getTreatmentWithConfig("test", Collections.emptyMap(), null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(1)).log(argumentCaptor.capture());
        assertEquals("exception", argumentCaptor.getValue().appliedRule());
        assertEquals("control", argumentCaptor.getValue().treatment());
    }

    @Test
    public void getTreatmentsWithConfigLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);

        Map<String, SplitResult> treatments = treatmentManager.getTreatmentsWithConfig(Arrays.asList("test", "test2"), Collections.emptyMap(), null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(0).appliedRule());
        assertEquals("control", allValues.get(0).treatment());
        assertEquals("default", allValues.get(1).appliedRule());
        assertEquals("on", allValues.get(1).treatment());
        assertEquals(2, treatments.size());
        assertEquals("control", treatments.get("test").treatment());
        assertEquals("on", treatments.get("test2").treatment());
    }

    @Test
    public void getTreatmentsByFlagSetLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mSplitsStorage.getNamesByFlagSets(any())).thenReturn(new HashSet<>(Arrays.asList("test", "test2")));
        when(mFlagSetsValidator.items(any(), any(), any())).thenReturn(Collections.singleton("set"));

        Map<String, String> treatments = treatmentManager.getTreatmentsByFlagSet("set", null, null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(1).appliedRule());
        assertEquals("control", allValues.get(1).treatment());
        assertEquals("default", allValues.get(0).appliedRule());
        assertEquals("on", allValues.get(0).treatment());
        assertEquals(2, treatments.size());
        assertEquals("control", treatments.get("test"));
        assertEquals("on", treatments.get("test2"));
    }

    @Test
    public void getTreatmentsByFlagSetsLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mSplitsStorage.getNamesByFlagSets(any())).thenReturn(new HashSet<>(Arrays.asList("test", "test2")));
        when(mFlagSetsValidator.items(any(), any(), any())).thenReturn(Collections.singleton("set"));

        Map<String, String> treatments = treatmentManager.getTreatmentsByFlagSets(Collections.singletonList("set"), null, null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(1).appliedRule());
        assertEquals("control", allValues.get(1).treatment());
        assertEquals("default", allValues.get(0).appliedRule());
        assertEquals("on", allValues.get(0).treatment());
        assertEquals(2, treatments.size());
        assertEquals("control", treatments.get("test"));
        assertEquals("on", treatments.get("test2"));
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mSplitsStorage.getNamesByFlagSets(any())).thenReturn(new HashSet<>(Arrays.asList("test", "test2")));
        when(mFlagSetsValidator.items(any(), any(), any())).thenReturn(Collections.singleton("set"));

        Map<String, SplitResult> treatments = treatmentManager.getTreatmentsWithConfigByFlagSet("set", null, null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(1).appliedRule());
        assertEquals("control", allValues.get(1).treatment());
        assertEquals("default", allValues.get(0).appliedRule());
        assertEquals("on", allValues.get(0).treatment());
        assertEquals(2, treatments.size());
        assertEquals("control", treatments.get("test").treatment());
        assertEquals("on", treatments.get("test2").treatment());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsLogsImpressionWithExceptionLabelWhenExceptionOccurs() {
        when(evaluator.getTreatment(anyString(), anyString(), eq("test"), anyMap())).thenThrow(new RuntimeException("test"));
        when(evaluator.getTreatment(anyString(), anyString(), eq("test2"), anyMap())).thenReturn(new EvaluationResult("on", "default"));
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mSplitsStorage.getNamesByFlagSets(any())).thenReturn(new HashSet<>(Arrays.asList("test", "test2")));
        when(mFlagSetsValidator.items(any(), any(), any())).thenReturn(Collections.singleton("set"));

        Map<String, SplitResult> treatments = treatmentManager.getTreatmentsWithConfigByFlagSets(Collections.singletonList("set"), null, null, false);
        ArgumentCaptor<Impression> argumentCaptor = ArgumentCaptor.forClass(Impression.class);

        verify(impressionListener, times(2)).log(argumentCaptor.capture());
        List<Impression> allValues = argumentCaptor.getAllValues();
        assertEquals("exception", allValues.get(1).appliedRule());
        assertEquals("control", allValues.get(1).treatment());
        assertEquals("default", allValues.get(0).appliedRule());
        assertEquals("on", allValues.get(0).treatment());
        assertEquals(2, treatments.size());
    }
}
