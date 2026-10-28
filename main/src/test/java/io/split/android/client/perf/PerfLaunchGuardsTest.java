package io.split.android.client.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PerfLaunchGuardsTest {

    @Test
    public void splitNamesShortOfExpectedFailsWithTheCounts() {
        assertFails("splitNames() after ready-from-cache: 999, expected 1000",
                () -> PerfLaunchGuards.requireSplitNames(1000, 999));
        assertFails("splitNames() after ready-from-cache: 0, expected 1000",
                () -> PerfLaunchGuards.requireSplitNames(1000, 0));
    }

    @Test
    public void splitNamesMatchingExpectedPasses() {
        PerfLaunchGuards.requireSplitNames(1000, 1000);
    }

    @Test
    public void networkCachePathFails() {
        IllegalStateException expected = assertThrows(IllegalStateException.class,
                () -> PerfLaunchGuards.requireCachePath("network"));
        // the message must name the offending path
        assertTrue(expected.getMessage().contains("network"));
    }

    @Test
    public void cacheAndReplayPathsPass() {
        PerfLaunchGuards.requireCachePath("cache");
        PerfLaunchGuards.requireCachePath("replay");
    }

    @Test
    public void missingFirstSinceFails() {
        IllegalStateException expected = assertThrows(IllegalStateException.class,
                () -> PerfLaunchGuards.requireFirstSince(null, 5L));
        assertTrue(expected.getMessage().contains("no flags sync"));
    }

    @Test
    public void wrongFirstSinceFailsWithBothValues() {
        assertFails("first flags sync since is 4, expected 5",
                () -> PerfLaunchGuards.requireFirstSince(4L, 5L));
    }

    @Test
    public void expectedFirstSincePasses() {
        PerfLaunchGuards.requireFirstSince(5L, 5L);
    }

    private static void assertFails(String message, Runnable action) {
        try {
            action.run();
            fail("expected IllegalStateException: " + message);
        } catch (IllegalStateException expected) {
            assertEquals(message, expected.getMessage());
        }
    }
}
