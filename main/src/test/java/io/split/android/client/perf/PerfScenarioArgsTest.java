package io.split.android.client.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PerfScenarioArgsTest {

    @Test
    public void nullArgumentsTakeDefaults() {
        PerfScenarioArgs args = PerfScenarioArgs.parse(null, null, null);
        assertEquals(0, args.latencyMs);
        assertEquals(0, args.run);
        assertEquals("unknown", args.git);
    }

    @Test
    public void validValuesAreParsed() {
        PerfScenarioArgs args = PerfScenarioArgs.parse("500", "7", "abc1234");
        assertEquals(500, args.latencyMs);
        assertEquals(7, args.run);
        assertEquals("abc1234", args.git);
    }

    @Test
    public void anyNonNegativeLatencyIsAccepted() {
        assertEquals(123, PerfScenarioArgs.parse("123", null, null).latencyMs);
    }

    @Test
    public void malformedLatencyIsRejected() {
        assertRejected("latencyMs must be a non-negative number: abc", "abc", "0");
        assertRejected("latencyMs must be a non-negative number: -1", "-1", "0");
    }

    @Test
    public void malformedRunIsRejected() {
        assertRejected("run must be a non-negative number: x", "0", "x");
        assertRejected("run must be a non-negative number: -3", "0", "-3");
    }

    private static void assertRejected(String message, String latency, String run) {
        try {
            PerfScenarioArgs.parse(latency, run, null);
            fail("expected IllegalArgumentException: " + message);
        } catch (IllegalArgumentException expected) {
            assertEquals(message, expected.getMessage());
        }
    }
}
