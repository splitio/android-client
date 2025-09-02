package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.split.android.client.utils.logger.LogPrinterStub;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class FallbackTreatmentsConfigurationTest {

    @Test
    public void constructorSetsFields() {
        FallbackTreatment global = new FallbackTreatment("off");
        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("flagA", new FallbackTreatment("off"));

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .global(global)
                .byFlag(map)
                .build();

        assertSame(global, cfg.getGlobal());
        assertEquals(1, cfg.getByFlag().size());
        assertEquals("off", cfg.getByFlag().get("flagA").getTreatment());
    }

    @Test
    public void byFlagIsUnmodifiable() {
        FallbackTreatment global = new FallbackTreatment("off");
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("flagA", new FallbackTreatment("off"));

        FallbackTreatmentsConfiguration config = FallbackTreatmentsConfiguration.builder()
                .global(global)
                .byFlag(byFlag)
                .build();

        byFlag.put("flagB", new FallbackTreatment("on"));

        // config map must not change
        assertEquals(1, config.getByFlag().size());

        try {
            config.getByFlag().put("x", new FallbackTreatment("on"));
            throw new AssertionError("Map should be unmodifiable");
        } catch (UnsupportedOperationException expected) {

        }
    }

    @Test
    public void equalityAndHashCodeByValue() {
        FallbackTreatment global = new FallbackTreatment("off");
        Map<String, FallbackTreatment> a = new HashMap<>();
        a.put("flagA", new FallbackTreatment("off"));

        Map<String, FallbackTreatment> b = new HashMap<>();
        b.put("flagA", new FallbackTreatment("off"));

        FallbackTreatmentsConfiguration configOne = FallbackTreatmentsConfiguration.builder().global(global).byFlag(a).build();
        FallbackTreatmentsConfiguration configTwo = FallbackTreatmentsConfiguration.builder().global(global).byFlag(b).build();
        FallbackTreatmentsConfiguration configThree = FallbackTreatmentsConfiguration.builder().global((String) null).byFlag(b).build();

        assertEquals(configOne, configTwo);
        assertEquals(configOne.hashCode(), configTwo.hashCode());
        assertNotEquals(configOne, configThree);
        assertNotEquals(configOne.hashCode(), configThree.hashCode());
    }

    @Test
    public void globalStringOverloadBuildsFallbackWithNullConfig() {
        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .global("on")
                .build();

        FallbackTreatment global = cfg.getGlobal();
        assertEquals("on", global.getTreatment());
        assertNull(global.getConfig());
    }

    @Test
    public void byFlagStringMapOverloadBuildsFallbacksWithNullConfig() {
        Map<String, String> flagTreatments = new HashMap<>();
        flagTreatments.put("flagA", "on");
        flagTreatments.put("flagB", "off");

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .byFlagStrings(flagTreatments)
                .build();

        assertEquals(2, cfg.getByFlag().size());
        assertEquals("on", cfg.getByFlag().get("flagA").getTreatment());
        assertNull(cfg.getByFlag().get("flagA").getConfig());
        assertEquals("off", cfg.getByFlag().get("flagB").getTreatment());
        assertNull(cfg.getByFlag().get("flagB").getConfig());
    }

    @Test
    public void callingByFlagStringsAfterByFlagMergesResultsAndLogsWarning() {
        LogPrinterStub printer = new LogPrinterStub();
        Logger.instance().setPrinter(printer);
        Logger.instance().setLevel(SplitLogLevel.WARNING);

        Map<String, FallbackTreatment> first = new HashMap<>();
        first.put("flagA", new FallbackTreatment("off", "cfgA"));
        first.put("flagB", new FallbackTreatment("on"));

        Map<String, String> second = new HashMap<>();
        second.put("flagA", "on"); // should override flagA
        second.put("flagC", "off");

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .byFlag(first)
                .byFlagStrings(second)
                .build();

        assertEquals(3, cfg.getByFlag().size());
        assertEquals("on", cfg.getByFlag().get("flagA").getTreatment());
        assertNull(cfg.getByFlag().get("flagA").getConfig());
        assertEquals("on", cfg.getByFlag().get("flagB").getTreatment());
        assertEquals("off", cfg.getByFlag().get("flagC").getTreatment());
        assertNull(cfg.getByFlag().get("flagC").getConfig());

        ConcurrentLinkedDeque<String> warns = printer.getLoggedMessages().get(android.util.Log.WARN);
        assertFalse("Expected at least one warning", warns.isEmpty());
        boolean containsExpected = warns.stream().anyMatch(m -> m.contains("Overriding existing fallback for flag 'flagA'"));
        assertTrue("Expected warning mentioning overridden key 'flagA'", containsExpected);
    }

    @Test
    public void callingByFlagAfterByFlagStringsMergesResultsAndLogsWarning() {
        LogPrinterStub printer = new LogPrinterStub();
        Logger.instance().setPrinter(printer);
        Logger.instance().setLevel(SplitLogLevel.WARNING);

        Map<String, String> first = new HashMap<>();
        first.put("flagA", "off");
        first.put("flagB", "on");

        Map<String, FallbackTreatment> second = new HashMap<>();
        second.put("flagA", new FallbackTreatment("on", "cfgA")); // should override flagA
        second.put("flagC", new FallbackTreatment("off"));

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .byFlagStrings(first)
                .byFlag(second)
                .build();

        assertEquals(3, cfg.getByFlag().size());
        assertEquals("on", cfg.getByFlag().get("flagA").getTreatment());
        assertEquals("cfgA", cfg.getByFlag().get("flagA").getConfig());
        assertEquals("on", cfg.getByFlag().get("flagB").getTreatment());
        assertNull(cfg.getByFlag().get("flagB").getConfig());
        assertEquals("off", cfg.getByFlag().get("flagC").getTreatment());

        boolean warned = !printer.getLoggedMessages().get(android.util.Log.WARN).isEmpty();
        assertTrue("Expected a warning log when merging byFlag and byFlagStrings", warned);
    }

    @Test
    public void byFlagAndByFlagStrings_NoOverlap_NoWarning() {
        LogPrinterStub printer = new LogPrinterStub();
        Logger.instance().setPrinter(printer);
        Logger.instance().setLevel(SplitLogLevel.WARNING);

        Map<String, FallbackTreatment> first = new HashMap<>();
        first.put("flagA", new FallbackTreatment("off"));

        Map<String, String> second = new HashMap<>();
        second.put("flagB", "on");

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .byFlag(first)
                .byFlagStrings(second)
                .build();

        assertEquals(2, cfg.getByFlag().size());
        assertEquals("off", cfg.getByFlag().get("flagA").getTreatment());
        assertEquals("on", cfg.getByFlag().get("flagB").getTreatment());

        boolean warned = !printer.getLoggedMessages().get(android.util.Log.WARN).isEmpty();
        assertFalse("Did not expect a warning", warned);
    }
}
