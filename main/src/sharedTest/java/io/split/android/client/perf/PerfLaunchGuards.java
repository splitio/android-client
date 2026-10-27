package io.split.android.client.perf;

/**
 * Checks that a launch measured what it was meant to: a ready-from-cache that came from the cache
 * load, with every flag readable, and a first sync that started from the seeded change number.
 * Pure Java, so it runs on the JVM.
 */
public final class PerfLaunchGuards {

    private PerfLaunchGuards() {
    }

    public static void requireSplitNames(int expected, int actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    "splitNames() after ready-from-cache: " + actual + ", expected " + expected);
        }
    }

    /** Ready-from-cache that came from the network would time the sync, not the cache load. */
    public static void requireCachePath(String path) {
        if ("network".equals(path)) {
            throw new IllegalStateException(
                    "ready-from-cache path is network: it must come from the cache load");
        }
    }

    public static void requireFirstSince(Long actual, long expected) {
        if (actual == null) {
            throw new IllegalStateException("no flags sync reached the server: expected since " + expected);
        }
        if (actual != expected) {
            throw new IllegalStateException(
                    "first flags sync since is " + actual + ", expected " + expected);
        }
    }
}
