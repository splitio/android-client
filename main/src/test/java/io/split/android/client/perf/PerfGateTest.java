package io.split.android.client.perf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PerfGateTest {

    @Test
    public void unsetPerfArgumentSkipsThePerfRun() {
        assertFalse(PerfGate.isEnabled(null));
    }

    @Test
    public void anySetPerfArgumentEnablesThePerfRun() {
        assertTrue(PerfGate.isEnabled("1"));
        assertTrue(PerfGate.isEnabled(""));
    }
}
