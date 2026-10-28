package io.split.android.client.perf;

/**
 * Decides whether a perf run is requested. The instrumentation argument is {@code perf}; a perf run
 * happens only when it is set. Pure Java, so it runs on the JVM.
 */
public final class PerfGate {

    /** The instrumentation argument name, passed as {@code -e perf <value>}. */
    public static final String ARG = "perf";

    private PerfGate() {
    }

    /** True for any non-null argument value; an unset argument (null) skips the perf run. */
    public static boolean isEnabled(String perfArg) {
        return perfArg != null;
    }
}
