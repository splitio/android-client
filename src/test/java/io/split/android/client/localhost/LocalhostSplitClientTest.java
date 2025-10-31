package io.split.android.client.localhost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationOptions;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitResult;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.engine.experiments.SplitParser;

public class LocalhostSplitClientTest {

    @Mock
    private LocalhostSplitFactory mockFactory;
    @Mock
    private SplitClientContainer mockClientContainer;
    @Mock
    private SplitClientConfig mockConfig;
    @Mock
    private SplitsStorage mockSplitsStorage;
    @Mock
    private SplitEventsManager mockEventsManager;
    @Mock
    private SplitParser mockSplitParser;
    @Mock
    private AttributesManager mockAttributesManager;
    @Mock
    private AttributesMerger mockAttributesMerger;
    @Mock
    private TelemetryStorageProducer mockTelemetryStorageProducer;
    @Mock
    private FlagSetsFilter mockFlagSetsFilter;
    @Mock
    private TreatmentManager mockTreatmentManager;

    private Key testKey;
    private LocalhostSplitClient client;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testKey = new Key("test_matching_key", "test_bucketing_key");

        when(mockConfig.labelsEnabled()).thenReturn(true);
        when(mockConfig.impressionListener()).thenReturn(null);

        client = new LocalhostSplitClient(
                mockFactory,
                mockClientContainer,
                mockConfig,
                testKey,
                mockSplitsStorage,
                mockEventsManager,
                mockSplitParser,
                mockAttributesManager,
                mockAttributesMerger,
                mockTelemetryStorageProducer,
                mockFlagSetsFilter
        );
    }

    @Test
    public void getTreatmentWithoutAttributesUsesEmptyMap() {
        when(mockTreatmentManager.getTreatment(eq("feature"), anyMap(), eq(null), anyBoolean()))
                .thenReturn("off");
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        String result = client.getTreatment("feature");

        assertEquals("off", result);
        verify(mockTreatmentManager).getTreatment("feature", Collections.emptyMap(), null, false);
    }

    @Test
    public void getTreatmentWithAttributesCallsWithNullEvaluationOptions() {
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatment(eq("feature"), eq(attributes), eq(null), anyBoolean()))
                .thenReturn("on");
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        String result = client.getTreatment("feature", attributes);

        assertEquals("on", result);
        verify(mockTreatmentManager).getTreatment("feature", attributes, null, false);
    }

    @Test
    public void getTreatmentWithEvaluationOptionsDelegatesToTreatmentManager() {
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        when(mockTreatmentManager.getTreatment(eq("feature"), eq(attributes), eq(options), anyBoolean()))
                .thenReturn("on");
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        String result = client.getTreatment("feature", attributes, options);

        assertEquals("on", result);
        verify(mockTreatmentManager).getTreatment("feature", attributes, options, false);
    }

    @Test
    public void getTreatmentWithConfigWithAttributesCallsWithNullEvaluationOptions() {
        Map<String, Object> attributes = new HashMap<>();
        SplitResult expectedResult = new SplitResult("on", "{\"color\":\"red\"}");
        when(mockTreatmentManager.getTreatmentWithConfig(eq("feature"), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expectedResult);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        SplitResult result = client.getTreatmentWithConfig("feature", attributes);

        assertEquals("on", result.treatment());
        assertEquals("{\"color\":\"red\"}", result.config());
        verify(mockTreatmentManager).getTreatmentWithConfig("feature", attributes, null, false);
    }

    @Test
    public void getTreatmentWithConfigDelegatesToTreatmentManager() {
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        SplitResult expectedResult = new SplitResult("on", "{\"color\":\"blue\"}");
        when(mockTreatmentManager.getTreatmentWithConfig(eq("feature"), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expectedResult);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        SplitResult result = client.getTreatmentWithConfig("feature", attributes, options);

        assertEquals("on", result.treatment());
        assertEquals("{\"color\":\"blue\"}", result.config());
        verify(mockTreatmentManager).getTreatmentWithConfig("feature", attributes, options, false);
    }

    @Test
    public void getTreatmentsWithAttributesCallsWithNullEvaluationOptions() {
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        expected.put("feature2", "off");
        when(mockTreatmentManager.getTreatments(eq(features), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatments(features, attributes);

        assertEquals(2, results.size());
        assertEquals("on", results.get("feature1"));
        assertEquals("off", results.get("feature2"));
        verify(mockTreatmentManager).getTreatments(features, attributes, null, false);
    }

    @Test
    public void getTreatmentsDelegatesToTreatmentManager() {
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        expected.put("feature2", "off");
        when(mockTreatmentManager.getTreatments(eq(features), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatments(features, attributes, options);

        assertEquals(2, results.size());
        assertEquals("on", results.get("feature1"));
        assertEquals("off", results.get("feature2"));
        verify(mockTreatmentManager).getTreatments(features, attributes, options, false);
    }

    @Test
    public void getTreatmentsWithConfigWithAttributesCallsWithNullEvaluationOptions() {
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on", "{\"test\":true}"));
        expected.put("feature2", new SplitResult("off"));
        when(mockTreatmentManager.getTreatmentsWithConfig(eq(features), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfig(features, attributes);

        assertEquals(2, results.size());
        assertEquals("on", results.get("feature1").treatment());
        assertEquals("{\"test\":true}", results.get("feature1").config());
        verify(mockTreatmentManager).getTreatmentsWithConfig(features, attributes, null, false);
    }

    @Test
    public void getTreatmentsWithConfigDelegatesToTreatmentManager() {
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on", "{\"size\":10}"));
        expected.put("feature2", new SplitResult("off"));
        when(mockTreatmentManager.getTreatmentsWithConfig(eq(features), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfig(features, attributes, options);

        assertEquals(2, results.size());
        assertEquals("on", results.get("feature1").treatment());
        assertEquals("{\"size\":10}", results.get("feature1").config());
        verify(mockTreatmentManager).getTreatmentsWithConfig(features, attributes, options, false);
    }

    @Test
    public void getTreatmentsByFlagSetWithAttributesCallsWithNullEvaluationOptions() {
        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        when(mockTreatmentManager.getTreatmentsByFlagSet(eq("backend"), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatmentsByFlagSet("backend", attributes);

        assertEquals(1, results.size());
        assertEquals("on", results.get("feature1"));
        verify(mockTreatmentManager).getTreatmentsByFlagSet("backend", attributes, null, false);
    }

    @Test
    public void getTreatmentsByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        when(mockTreatmentManager.getTreatmentsByFlagSet(eq("backend"), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatmentsByFlagSet("backend", attributes, options);

        assertEquals(1, results.size());
        assertEquals("on", results.get("feature1"));
        verify(mockTreatmentManager).getTreatmentsByFlagSet("backend", attributes, options, false);
    }

    @Test
    public void getTreatmentsByFlagSetsWithAttributesCallsWithNullEvaluationOptions() {
        List<String> flagSets = Arrays.asList("backend", "frontend");
        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        expected.put("feature2", "off");
        when(mockTreatmentManager.getTreatmentsByFlagSets(eq(flagSets), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatmentsByFlagSets(flagSets, attributes);

        assertEquals(2, results.size());
        verify(mockTreatmentManager).getTreatmentsByFlagSets(flagSets, attributes, null, false);
    }

    @Test
    public void getTreatmentsByFlagSetsDelegatesToTreatmentManager() {
        List<String> flagSets = Arrays.asList("backend", "frontend");
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, String> expected = new HashMap<>();
        expected.put("feature1", "on");
        expected.put("feature2", "off");
        when(mockTreatmentManager.getTreatmentsByFlagSets(eq(flagSets), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, String> results = client.getTreatmentsByFlagSets(flagSets, attributes, options);

        assertEquals(2, results.size());
        verify(mockTreatmentManager).getTreatmentsByFlagSets(flagSets, attributes, options, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWithAttributesCallsWithNullEvaluationOptions() {
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on", "{\"version\":1}"));
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSet(eq("backend"), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSet("backend", attributes);

        assertEquals(1, results.size());
        assertEquals("on", results.get("feature1").treatment());
        verify(mockTreatmentManager).getTreatmentsWithConfigByFlagSet("backend", attributes, null, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on", "{\"version\":2}"));
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSet(eq("backend"), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSet("backend", attributes, options);

        assertEquals(1, results.size());
        assertEquals("on", results.get("feature1").treatment());
        verify(mockTreatmentManager).getTreatmentsWithConfigByFlagSet("backend", attributes, options, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithAttributesCallsWithNullEvaluationOptions() {
        List<String> flagSets = Arrays.asList("backend", "frontend");
        Map<String, Object> attributes = new HashMap<>();
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on"));
        expected.put("feature2", new SplitResult("off"));
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSets(eq(flagSets), eq(attributes), eq(null), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSets(flagSets, attributes);

        assertEquals(2, results.size());
        verify(mockTreatmentManager).getTreatmentsWithConfigByFlagSets(flagSets, attributes, null, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsDelegatesToTreatmentManager() {
        List<String> flagSets = Arrays.asList("backend", "frontend");
        Map<String, Object> attributes = new HashMap<>();
        EvaluationOptions options = mock(EvaluationOptions.class);
        Map<String, SplitResult> expected = new HashMap<>();
        expected.put("feature1", new SplitResult("on"));
        expected.put("feature2", new SplitResult("off"));
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSets(eq(flagSets), eq(attributes), eq(options), anyBoolean()))
                .thenReturn(expected);
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSets(flagSets, attributes, options);

        assertEquals(2, results.size());
        verify(mockTreatmentManager).getTreatmentsWithConfigByFlagSets(flagSets, attributes, options, false);
    }

    @Test
    public void destroyRemovesClientFromContainerAndDestroysFactory() {
        client.destroy();

        verify(mockClientContainer).remove(testKey);
        verify(mockFactory).destroy();
    }

    @Test
    public void isReadyReturnsTrueWhenSdkReadyEventTriggered() {
        when(mockEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);

        assertTrue(client.isReady());
        verify(mockEventsManager).eventAlreadyTriggered(SplitEvent.SDK_READY);
    }

    @Test
    public void isReadyReturnsFalseWhenSdkReadyEventNotTriggered() {
        when(mockEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(false);

        assertFalse(client.isReady());
        verify(mockEventsManager).eventAlreadyTriggered(SplitEvent.SDK_READY);
    }

    @Test
    public void onRegistersEventTaskWhenEventNotTriggered() {
        SplitEvent event = SplitEvent.SDK_READY;
        SplitEventTask task = mock(SplitEventTask.class);

        when(mockEventsManager.eventAlreadyTriggered(event)).thenReturn(false);

        client.on(event, task);

        verify(mockEventsManager).register(event, task);
    }

    @Test
    public void onRegistersTaskForSdkReadyFromCacheEvenIfAlreadyTriggered() {
        SplitEvent event = SplitEvent.SDK_READY_FROM_CACHE;
        SplitEventTask task = mock(SplitEventTask.class);

        when(mockEventsManager.eventAlreadyTriggered(event)).thenReturn(true);

        client.on(event, task);

        verify(mockEventsManager).register(event, task);
    }

    @Test
    public void onDoesNotRegisterEventTaskWhenEventAlreadyTriggered() {
        SplitEvent event = SplitEvent.SDK_READY;
        SplitEventTask task = mock(SplitEventTask.class);

        when(mockEventsManager.eventAlreadyTriggered(event)).thenReturn(true);

        client.on(event, task);

        verify(mockEventsManager, never()).register(any(), any());
    }

    @Test
    public void trackMethodsReturnFalse() {
        assertFalse(client.track("user", "event_type"));
        assertFalse(client.track("user", "event_type", 10.5));
        assertFalse(client.track("event_type"));
        assertFalse(client.track("event_type", 10.5));
        assertFalse(client.track("user", "event_type", new HashMap<>()));
        assertFalse(client.track("user", "event_type", 10.5, new HashMap<>()));
        assertFalse(client.track("event_type", new HashMap<>()));
        assertFalse(client.track("event_type", 10.5, new HashMap<>()));
    }

    @Test
    public void flushDoesNothing() {
        // This should not throw any exception
        client.flush();
    }

    @Test
    public void setAttributeReturnsTrue() {
        assertTrue(client.setAttribute("age", 25));
    }

    @Test
    public void getAttributeReturnsNull() {
        assertNull(client.getAttribute("age"));
    }

    @Test
    public void setAttributesReturnsTrue() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("age", 25);
        attributes.put("name", "John");

        assertTrue(client.setAttributes(attributes));
    }

    @Test
    public void getAllAttributesReturnsEmptyMap() {
        Map<String, Object> attributes = client.getAllAttributes();

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    public void removeAttributeReturnsTrue() {
        assertTrue(client.removeAttribute("age"));
    }

    @Test
    public void clearAttributesReturnsTrue() {
        assertTrue(client.clearAttributes());
    }

    @Test
    public void getTreatmentAfterDestroyPassesDestroyedFlag() {
        when(mockTreatmentManager.getTreatment(eq("feature"), anyMap(), eq(null), eq(true)))
                .thenReturn("control");
        client.setTreatmentManagerForTesting(mockTreatmentManager);

        client.destroy();
        String result = client.getTreatment("feature");

        verify(mockTreatmentManager).getTreatment("feature", Collections.emptyMap(), null, true);
    }

    @Test
    public void getTreatmentWhenTreatmentManagerThrowsReturnsFallback() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockTreatmentManager.getTreatment(eq("feature"), anyMap(), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        String result = client.getTreatment("feature");

        assertEquals("control", result);
        verify(mockCalculator).resolve("feature", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentWithConfigWhenTreatmentManagerThrowsReturnsFallback() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentWithConfig(eq("feature"), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        SplitResult result = client.getTreatmentWithConfig("feature", attributes);

        assertEquals("control", result.treatment());
        verify(mockCalculator).resolve("feature", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockCalculator.resolve(eq("feature2"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatments(eq(features), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, String> results = client.getTreatments(features, attributes);

        assertEquals(2, results.size());
        assertEquals("control", results.get("feature1"));
        assertEquals("control", results.get("feature2"));
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
        verify(mockCalculator).resolve("feature2", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsWithConfigWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockCalculator.resolve(eq("feature2"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        List<String> features = Arrays.asList("feature1", "feature2");
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentsWithConfig(eq(features), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, SplitResult> results = client.getTreatmentsWithConfig(features, attributes);

        assertEquals(2, results.size());
        assertEquals("control", results.get("feature1").treatment());
        assertEquals("control", results.get("feature2").treatment());
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
        verify(mockCalculator).resolve("feature2", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsByFlagSetWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockSplitsStorage.getNamesByFlagSets(eq(Collections.singletonList("backend"))))
                .thenReturn(Collections.singleton("feature1"));
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentsByFlagSet(eq("backend"), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, String> results = client.getTreatmentsByFlagSet("backend", attributes);

        assertEquals(1, results.size());
        assertEquals("control", results.get("feature1"));
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsByFlagSetsWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockCalculator.resolve(eq("feature2"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        List<String> flagSets = Arrays.asList("backend", "frontend");
        when(mockSplitsStorage.getNamesByFlagSets(eq(flagSets)))
                .thenReturn(Set.of("feature1", "feature2"));
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentsByFlagSets(eq(flagSets), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, String> results = client.getTreatmentsByFlagSets(flagSets, attributes);

        assertEquals(2, results.size());
        assertEquals("control", results.get("feature1"));
        assertEquals("control", results.get("feature2"));
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
        verify(mockCalculator).resolve("feature2", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockSplitsStorage.getNamesByFlagSets(eq(Collections.singletonList("backend"))))
                .thenReturn(Collections.singleton("feature1"));
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSet(eq("backend"), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSet("backend", attributes);

        assertEquals(1, results.size());
        assertEquals("control", results.get("feature1").treatment());
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWhenTreatmentManagerThrowsReturnsFallbacks() {
        FallbackTreatmentsCalculator mockCalculator = mock(FallbackTreatmentsCalculator.class);
        when(mockCalculator.resolve(eq("feature1"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        when(mockCalculator.resolve(eq("feature2"), eq(TreatmentLabels.EXCEPTION)))
                .thenReturn(new FallbackTreatment("control"));
        List<String> flagSets = Arrays.asList("backend", "frontend");
        when(mockSplitsStorage.getNamesByFlagSets(eq(flagSets)))
                .thenReturn(Set.of("feature1", "feature2"));
        Map<String, Object> attributes = new HashMap<>();
        when(mockTreatmentManager.getTreatmentsWithConfigByFlagSets(eq(flagSets), eq(attributes), eq(null), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));
        client.setTreatmentManagerForTesting(mockTreatmentManager);
        client.setFallbackTreatmentsCalculatorForTesting(mockCalculator);

        Map<String, SplitResult> results = client.getTreatmentsWithConfigByFlagSets(flagSets, attributes);

        assertEquals(2, results.size());
        assertEquals("control", results.get("feature1").treatment());
        assertEquals("control", results.get("feature2").treatment());
        verify(mockCalculator).resolve("feature1", TreatmentLabels.EXCEPTION);
        verify(mockCalculator).resolve("feature2", TreatmentLabels.EXCEPTION);
    }
}
