package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FallbackConfigurationTest {

    @Test
    public void constructorSetsFields() {
        FallbackTreatment global = new FallbackTreatment("off");
        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("flagA", new FallbackTreatment("off"));

        FallbackConfiguration cfg = FallbackConfiguration.builder()
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

        FallbackConfiguration config = FallbackConfiguration.builder()
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

        FallbackConfiguration configOne = FallbackConfiguration.builder().global(global).byFlag(a).build();
        FallbackConfiguration configTwo = FallbackConfiguration.builder().global(global).byFlag(b).build();
        FallbackConfiguration configThree = FallbackConfiguration.builder().global(null).byFlag(b).build();

        assertEquals(configOne, configTwo);
        assertEquals(configOne.hashCode(), configTwo.hashCode());
        assertNotEquals(configOne, configThree);
        assertNotEquals(configOne.hashCode(), configThree.hashCode());
    }
}
