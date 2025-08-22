package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FallbackTreatmentTest {

    private static final String FALLBACK_TREATMENT = "fallback treatment";

    @Test
    public void constructorSetsFields() {
        FallbackTreatment ft = new FallbackTreatment("off", "{\"k\":true}");
        assertEquals("off", ft.getTreatment());
        assertEquals("{\"k\":true}", ft.getConfig());
        assertEquals(FALLBACK_TREATMENT, ft.getLabel());
    }

    @Test
    public void configCanBeNull() {
        FallbackTreatment ft = new FallbackTreatment("off", null);
        assertEquals("off", ft.getTreatment());
        assertNull(ft.getConfig());
        assertEquals(FALLBACK_TREATMENT, ft.getLabel());
    }

    @Test
    public void convenienceConstructorSetsNullConfig() {
        FallbackTreatment ft = new FallbackTreatment("off");
        assertEquals("off", ft.getTreatment());
        assertNull(ft.getConfig());
        assertEquals(FALLBACK_TREATMENT, ft.getLabel());
    }

    @Test
    public void equalityAndHashCodeByValue() {
        FallbackTreatment a = new FallbackTreatment("off", null);
        FallbackTreatment b = new FallbackTreatment("off", null);
        FallbackTreatment c = new FallbackTreatment("on", null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
