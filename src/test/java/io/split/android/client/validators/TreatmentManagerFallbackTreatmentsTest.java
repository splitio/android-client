package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.SplitResult;
import io.split.android.client.dtos.Split;
import io.split.android.client.fallback.FallbackConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;
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
}
