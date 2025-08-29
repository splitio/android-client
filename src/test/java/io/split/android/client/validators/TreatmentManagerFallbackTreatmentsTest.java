package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.TreatmentLabels;
import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.SplitResult;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.dtos.Split;
import io.split.android.client.fallback.FallbackConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;
import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;

public class TreatmentManagerFallbackTreatmentsTest {

    private static final String FLAG = "missing_flag";

    @Test
    public void evaluatorDefinitionNotFoundUsesFallback() {
        FallbackConfiguration cfg = FallbackConfiguration.builder()
                .global(new FallbackTreatment("FALLBACK_TREATMENT", "{\"k\":1}"))
                .build();
        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(cfg);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        SplitParser splitParser = Mockito.mock(SplitParser.class);
        when(splitsStorage.get(FLAG)).thenReturn(null); // definition not found
        when(splitParser.parse(null, "m")).thenReturn(null);

        EvaluatorImpl evaluator = new EvaluatorImpl(splitsStorage, splitParser, calc);

        EvaluationResult res = evaluator.getTreatment("m", null, FLAG, Collections.emptyMap());

        assertEquals("FALLBACK_TREATMENT", res.getTreatment());
        assertTrue(res.getLabel().startsWith("fallback - "));
        assertEquals("{\"k\":1}", res.getConfigurations());
    }

    @Test
    public void evaluatorExceptionUsesFallback() {
        FallbackConfiguration cfg = FallbackConfiguration.builder()
                .global(new FallbackTreatment("FALLBACK_TREATMENT_2"))
                .build();
        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(cfg);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        SplitParser splitParser = Mockito.mock(SplitParser.class);
        Split dtoSplit = Mockito.mock(Split.class);
        when(splitsStorage.get(FLAG)).thenReturn(dtoSplit);
        when(splitParser.parse(Mockito.eq(dtoSplit), Mockito.eq("m"))).thenThrow(new RuntimeException("boom"));

        EvaluatorImpl evaluator = new EvaluatorImpl(splitsStorage, splitParser, calc);

        EvaluationResult res = evaluator.getTreatment("m", null, FLAG, Collections.emptyMap());

        assertEquals("FALLBACK_TREATMENT_2", res.getTreatment());
        assertTrue(res.getLabel().startsWith("fallback - "));
    }

    @Test
    public void helperControlTreatmentsPathUsesFallback() {
        FallbackConfiguration cfg = FallbackConfiguration.builder()
                .global(new FallbackTreatment("FALLBACK_HELPER", "cfg"))
                .build();
        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(cfg);

        SplitValidator okValidator = new SplitValidatorImpl();
        ValidationMessageLogger logger = new ValidationMessageLoggerImpl();

        List<String> names = Arrays.asList(" flag_a ", "flag_b");
        Map<String, SplitResult> out = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(
                okValidator,
                logger,
                names,
                "test",
                TreatmentManagerImpl.ResultTransformer::identity,
                calc);

        assertEquals(2, out.size());
        assertEquals("FALLBACK_HELPER", out.get("flag_a").treatment());
        assertEquals("cfg", out.get("flag_a").config());
        assertEquals("FALLBACK_HELPER", out.get("flag_b").treatment());
    }

    @Test
    public void treatmentManagerGetTreatmentNullStringUsesFallback() {
        String flag = "flag_for_null";

        Evaluator evaluator = mock(Evaluator.class);
        TelemetryStorageProducer telemetry = mock(TelemetryStorageProducer.class);
        FallbackTreatmentsCalculator fallbackCalc = mock(FallbackTreatmentsCalculator.class);
        Mocks m = Mocks.create(evaluator, telemetry, fallbackCalc);

        when(evaluator.getTreatment(eq("m"), eq("b"), eq(flag), any()))
                .thenReturn(new EvaluationResult(null, "label", null, null, false));

        when(fallbackCalc.resolve(flag)).thenReturn(new FallbackTreatment("FALLBACK_TMT"));

        String out = m.manager.getTreatment(flag, null, null, false);

        assertEquals("FALLBACK_TMT", out);
        verify(fallbackCalc, times(1)).resolve(flag);
    }

    @Test
    public void treatmentManagerGetTreatmentExceptionRecordsTelemetryAndUsesFallback() {
        String flag = "flag_for_exception";

        Evaluator evaluator = mock(Evaluator.class);
        TelemetryStorageProducer telemetry = mock(TelemetryStorageProducer.class);
        FallbackTreatmentsCalculator fallbackCalc = mock(FallbackTreatmentsCalculator.class);
        Mocks m = Mocks.create(evaluator, telemetry, fallbackCalc);

        when(evaluator.getTreatment(eq("m"), eq("b"), eq(flag), any()))
                .thenReturn(new EvaluationResult(null, "label", null, null, false));

        when(fallbackCalc.resolve(flag))
                .thenThrow(new RuntimeException("fail once"))
                .thenReturn(new FallbackTreatment("FALLBACK_AFTER_EXCEPTION"));

        String out = m.manager.getTreatment(flag, null, null, false);

        assertEquals("FALLBACK_AFTER_EXCEPTION", out);
        verify(telemetry, times(1)).recordException(Method.TREATMENT);
        verify(fallbackCalc, times(2)).resolve(flag);
    }

    @Test
    public void treatmentManagerLabelContainsDefinitionNotFoundTriggersNotFoundPath() {
        String flag = "flag_contains_def_not_found";

        Evaluator evaluator = mock(Evaluator.class);
        TelemetryStorageProducer telemetry = mock(TelemetryStorageProducer.class);
        FallbackTreatmentsCalculator fallbackCalc = mock(FallbackTreatmentsCalculator.class);
        Mocks m = Mocks.create(evaluator, telemetry, fallbackCalc);

        String label = "some prefix - " + TreatmentLabels.DEFINITION_NOT_FOUND + " - some suffix";
        when(evaluator.getTreatment(eq("m"), eq("b"), eq(flag), any()))
                .thenReturn(new EvaluationResult("on", label, null, null, false));

        when(m.splitValidator.splitNotFoundMessage(flag)).thenReturn("not found: " + flag);

        // Invoke getTreatmentWithConfig to go through getTreatmentWithConfigWithoutMetrics path
        SplitResult result = m.manager.getTreatmentWithConfig(flag, null, null, false);

        // Ensure treatment is the one provided by evaluator and no impressions are logged
        assertEquals("on", result.treatment());
        verify(m.impressions, times(0)).log(Mockito.any(DecoratedImpression.class));
        verify(m.impressions, times(0)).log(Mockito.any(Impression.class));

        // Ensure we logged the not-found warning by requesting the message from SplitValidator
        verify(m.splitValidator, times(1)).splitNotFoundMessage(flag);
    }

    private static class Mocks {
        final TreatmentManagerImpl manager;
        final KeyValidator keyValidator;
        final SplitValidator splitValidator;
        final ImpressionListener.FederatedImpressionListener impressions;
        final ListenableEventsManager events;
        final AttributesManager attributesManager;
        final AttributesMerger attributesMerger;
        final FlagSetsFilter flagSetsFilter;
        final SplitsStorage splitsStorage;
        final SplitFilterValidator flagSetsValidator;
        final PropertyValidator propertyValidator;

        private Mocks(TreatmentManagerImpl manager,
                      KeyValidator keyValidator,
                      SplitValidator splitValidator,
                      ImpressionListener.FederatedImpressionListener impressions,
                      ListenableEventsManager events,
                      AttributesManager attributesManager,
                      AttributesMerger attributesMerger,
                      FlagSetsFilter flagSetsFilter,
                      SplitsStorage splitsStorage,
                      SplitFilterValidator flagSetsValidator,
                      PropertyValidator propertyValidator) {
            this.manager = manager;
            this.keyValidator = keyValidator;
            this.splitValidator = splitValidator;
            this.impressions = impressions;
            this.events = events;
            this.attributesManager = attributesManager;
            this.attributesMerger = attributesMerger;
            this.flagSetsFilter = flagSetsFilter;
            this.splitsStorage = splitsStorage;
            this.flagSetsValidator = flagSetsValidator;
            this.propertyValidator = propertyValidator;
        }

        static Mocks create(Evaluator evaluator,
                            TelemetryStorageProducer telemetry,
                            FallbackTreatmentsCalculator fallbackCalc) {
            KeyValidator keyValidator = mock(KeyValidator.class);
            SplitValidator splitValidator = mock(SplitValidator.class);
            ImpressionListener.FederatedImpressionListener impressions = mock(ImpressionListener.FederatedImpressionListener.class);
            ListenableEventsManager events = mock(ListenableEventsManager.class);
            AttributesManager attributesManager = mock(AttributesManager.class);
            AttributesMerger attributesMerger = mock(AttributesMerger.class);
            FlagSetsFilter flagSetsFilter = mock(FlagSetsFilter.class);
            SplitsStorage splitsStorage = mock(SplitsStorage.class);
            ValidationMessageLogger validationLogger = new ValidationMessageLoggerImpl();
            SplitFilterValidator flagSetsValidator = mock(SplitFilterValidator.class);
            PropertyValidator propertyValidator = mock(PropertyValidator.class);

            when(events.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
            when(attributesManager.getAllAttributes()).thenReturn(Collections.emptyMap());
            when(attributesMerger.merge(any(), any())).thenReturn(Collections.emptyMap());
            when(splitValidator.validateName(any())).thenReturn(null);
            when(keyValidator.validate(any(), any())).thenReturn(null);

            TreatmentManagerImpl manager = new TreatmentManagerImpl(
                    "m",
                    "b",
                    evaluator,
                    keyValidator,
                    splitValidator,
                    impressions,
                    true,
                    events,
                    attributesManager,
                    attributesMerger,
                    telemetry,
                    flagSetsFilter,
                    splitsStorage,
                    validationLogger,
                    flagSetsValidator,
                    propertyValidator,
                    fallbackCalc);

            return new Mocks(manager, keyValidator, splitValidator, impressions, events, attributesManager,
                    attributesMerger, flagSetsFilter, splitsStorage, flagSetsValidator, propertyValidator);
        }
    }
}
