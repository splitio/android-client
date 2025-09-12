package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FallbackTreatmentTest {

    @Test
    public void constructorSetsFields() {
        FallbackTreatment ft = new FallbackTreatment("off", "{\"k\":true}", "my label");
        assertEquals("off", ft.getTreatment());
        assertEquals("{\"k\":true}", ft.getConfig());
        assertEquals("my label", ft.getLabel());
    }

    @Test
    public void configCanBeNull() {
        FallbackTreatment ft = new FallbackTreatment("off", null, "my label");
        assertEquals("off", ft.getTreatment());
        assertNull(ft.getConfig());
        assertEquals("my label", ft.getLabel());
    }

    @Test
    public void labelCanBeNull() {
        FallbackTreatment ft = new FallbackTreatment("off", null, null);
        assertEquals("off", ft.getTreatment());
        assertNull(ft.getConfig());
        assertNull(ft.getLabel());
    }

    @Test
    public void convenienceConstructorSetsNullConfigAndLabel() {
        FallbackTreatment ft = new FallbackTreatment("off");
        assertEquals("off", ft.getTreatment());
        assertNull(ft.getConfig());
        assertNull(ft.getLabel());
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
