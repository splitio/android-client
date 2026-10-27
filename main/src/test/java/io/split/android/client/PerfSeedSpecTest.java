package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.split.android.client.PerfSeedSpec.Spec;

public class PerfSeedSpecTest {

    @Test
    public void requireCountFailsOnAnEmptySeedAndNamesTableActualAndExpected() {
        try {
            PerfSeedSpec.requireCount("splits", 0, 1000);
            fail("an empty seed must not pass the count guard");
        } catch (IllegalStateException e) {
            assertEquals("splits: 0 rows, expected 1000", e.getMessage());
        }
    }

    @Test
    public void requireCountPassesWhenTheCountsMatch() {
        PerfSeedSpec.requireCount("splits", 1000, 1000);
    }

    @Test
    public void requireCountFailsWhenThereAreTooManyRows() {
        try {
            PerfSeedSpec.requireCount("rule_based_segments", 51, 50);
            fail("a surplus row must not pass the count guard");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("rule_based_segments"));
        }
    }

    @Test
    public void requireLongFailsOnAMismatchAndPassesOnEqual() {
        PerfSeedSpec.requireLong("splitChangesChangeNumber", 7L, 7L);
        try {
            PerfSeedSpec.requireLong("splitChangesChangeNumber", -1L, 1_700_000_000_000L);
            fail("a mismatched value must not pass");
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("splitChangesChangeNumber"));
            assertTrue(message, message.contains("-1"));
            assertTrue(message, message.contains("1700000000000"));
        }
    }

    @Test
    public void requireLongFailsWhenTheValueIsAboveTheExpectedOne() {
        try {
            PerfSeedSpec.requireLong("rbsChangeNumber", 9L, 5L);
            fail("a larger value must not pass");
        } catch (IllegalStateException e) {
            assertEquals("rbsChangeNumber is 9, expected 5", e.getMessage());
        }
    }

    @Test
    public void databaseNameIsTheOneTheFactoryDerivesFromTheKey() {
        SplitClientConfig config = SplitClientConfig.builder().build();

        assertEquals(SplitFactoryHelper.buildDatabaseName(config, PerfSeedSpec.SDK_KEY),
                PerfSeedSpec.databaseName(config));
        assertEquals("perf0001", PerfSeedSpec.databaseName(config));
    }

    @Test
    public void databaseNameFollowsTheConfigPrefix() {
        SplitClientConfig config = SplitClientConfig.builder().prefix("p_").build();

        assertEquals("p_perf0001", PerfSeedSpec.databaseName(config));
    }

    @Test
    public void sdkKeyIsLongEnoughForTheCipherIv() {
        assertEquals(27, PerfSeedSpec.SDK_KEY.length());
    }

    @Test
    public void ruleBasedSegmentsAreOneTwentiethOfTheFlagsWithAMinimumOfOne() {
        assertEquals(18, new Spec(false, 373, false).rbs);
        assertEquals(5, new Spec(false, 100, false).rbs);
        assertEquals(50, new Spec(false, 1000, false).rbs);
        assertEquals(250, new Spec(false, 5000, false).rbs);
        assertEquals(1, new Spec(false, 7, false).rbs);
    }

    @Test
    public void idEncodesEncryptionFlagsAndSets() {
        assertEquals("enc0-n1000-sets0", new Spec(false, 1000, false).id());
        assertEquals("enc1-n373-sets1", new Spec(true, 373, true).id());
    }

    @Test
    public void flagNamesArePaddedToFiveDigits() {
        assertEquals("flag_00000", PerfSeedSpec.flagName(0));
        assertEquals("flag_04999", PerfSeedSpec.flagName(4999));
    }

    @Test
    public void absentArgumentsGiveTheDefaults() {
        Spec spec = Spec.parse(null, null, null);

        assertFalse(spec.encrypted);
        assertEquals(1000, spec.flags);
        assertFalse(spec.flagSets);
    }

    @Test
    public void argumentsAreParsed() {
        Spec spec = Spec.parse("1", "373", "1");

        assertTrue(spec.encrypted);
        assertEquals(373, spec.flags);
        assertTrue(spec.flagSets);
        assertEquals("enc1-n373-sets1", spec.id());
    }

    @Test
    public void badArgumentsAreRejected() {
        assertRejected("2", "100", "0");
        assertRejected("true", "100", "0");
        assertRejected("0", "100", "2");
        assertRejected("0", "0", "0");
        assertRejected("0", "-5", "0");
        assertRejected("0", "many", "0");
    }

    private static void assertRejected(String enc, String flags, String sets) {
        try {
            Spec.parse(enc, flags, sets);
            fail("expected a rejection for enc=" + enc + " flags=" + flags + " sets=" + sets);
        } catch (IllegalArgumentException expected) {
            // rejected as required
        }
    }
}
