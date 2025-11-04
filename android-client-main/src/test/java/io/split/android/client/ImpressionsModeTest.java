package io.split.android.client;

import static org.junit.Assert.*;

import org.junit.Test;

import io.split.android.client.service.impressions.ImpressionsMode;

public class ImpressionsModeTest {

    @Test
    public void testDebugMode() {
        assertEquals(ImpressionsMode.DEBUG, ImpressionsMode.fromString("DEBUG"));
    }

    @Test
    public void testOptimizedMode() {
        assertEquals(ImpressionsMode.OPTIMIZED, ImpressionsMode.fromString("OPTIMIZED"));
    }

    @Test
    public void testNoneMode() {
        assertEquals(ImpressionsMode.NONE, ImpressionsMode.fromString("NONE"));
    }

    @Test
    public void defaultModeIsOptimized() {
        assertEquals(ImpressionsMode.OPTIMIZED, ImpressionsMode.fromString("random"));
    }
}
