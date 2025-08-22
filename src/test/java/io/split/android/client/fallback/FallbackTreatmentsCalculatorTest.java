package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FallbackTreatmentsCalculatorTest {

    @Test
    public void flagLevelOverrideTakesPrecedence() {
        FallbackTreatment global = new FallbackTreatment("off", "{\"g\":true}");
        FallbackTreatment byFlag = new FallbackTreatment("on", "{\"f\":true}");
        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("my_flag", byFlag);
        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(global)
                .byFlag(map)
                .build();

        FallbackTreatmentsCalculator calculator = new FallbackTreatmentsCalculatorImpl(config);
        FallbackTreatment resolvedExisting = calculator.resolve("my_flag");
        FallbackTreatment resolvedOther = calculator.resolve("other_flag");

        assertNotNull(resolvedExisting);
        assertEquals(byFlag, resolvedExisting);
        assertNotNull(resolvedOther);
        assertEquals(global, resolvedOther);
    }

    @Test
    public void globalFallbackIsReturnedWhenNoFlagOverride() {
        FallbackTreatment global = new FallbackTreatment("off");
        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(global)
                .byFlag(Collections.emptyMap())
                .build();

        FallbackTreatmentsCalculator calculator = new FallbackTreatmentsCalculatorImpl(config);
        FallbackTreatment resolved = calculator.resolve("any_flag");

        assertNotNull(resolved);
        assertEquals(global, resolved);
    }

    @Test
    public void flagLevelFallbackIsReturnedWhenConfigured() {
        FallbackTreatment byFlag = new FallbackTreatment("on");
        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("flagA", byFlag);
        FallbackConfiguration config = FallbackConfiguration.builder()
                .byFlag(map)
                .build();

        FallbackTreatmentsCalculator calculator = new FallbackTreatmentsCalculatorImpl(config);
        FallbackTreatment resolved = calculator.resolve("flagA");

        assertNotNull(resolved);
        assertEquals(byFlag, resolved);
    }

    @Test
    public void returnsControlWhenNoFallbackConfigured() {
        FallbackConfiguration config = FallbackConfiguration.builder()
                .build();

        FallbackTreatmentsCalculator calculator = new FallbackTreatmentsCalculatorImpl(config);
        FallbackTreatment resolved = calculator.resolve("nope");

        assertNotNull(resolved);
        assertEquals(FallbackTreatment.CONTROL, resolved);
    }

    @Test
    public void nonexistentFlagFallsBackToGlobal() {
        FallbackTreatment global = new FallbackTreatment("off");
        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("flagA", new FallbackTreatment("on"));
        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(global)
                .byFlag(map)
                .build();

        FallbackTreatmentsCalculator calculator = new FallbackTreatmentsCalculatorImpl(config);
        FallbackTreatment resolved = calculator.resolve("flagB");

        assertNotNull(resolved);
        assertEquals(global, resolved);
    }
}
